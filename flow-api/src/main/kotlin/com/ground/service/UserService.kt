package com.ground.service

import com.ground.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * UserService - Manages user accounts and authentication.
 * 
 * This is the new user management service for RCAC.
 * Users are separate from businesses - they can belong to multiple businesses.
 */
class UserService(
    private val auditService: AuditService = AuditService()
) {
    companion object {
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15L
    }
    
    // ═══════════════════════════════════════════════════════════════
    // USER CRUD
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create a new user account.
     */
    fun createUser(
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): UserResponse {
        return transaction {
            val now = Instant.now()
            val normalizedEmail = email.lowercase().trim()
            
            // Check if email already exists
            val existing = Users.select { Users.email eq normalizedEmail }.firstOrNull()
            require(existing == null) { "An account with this email already exists" }
            
            // Hash password
            val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
            
            val userId = Users.insertAndGetId {
                it[Users.email] = normalizedEmail
                it[Users.passwordHash] = passwordHash
                it[Users.firstName] = firstName
                it[Users.lastName] = lastName
                it[status] = "active"
                it[createdAt] = now
                it[updatedAt] = now
            }.value
            
            auditService.logUserAction(
                actorUserId = userId,
                action = "user_created",
                metadata = mapOf("email" to normalizedEmail),
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            
            getUserResponse(userId)!!
        }
    }
    
    /**
     * Get a user by ID.
     */
    fun getUser(userId: UUID): UserResponse? {
        return transaction {
            getUserResponse(userId)
        }
    }
    
    /**
     * Get a user by email.
     */
    fun getUserByEmail(email: String): UserResponse? {
        return transaction {
            Users.select { Users.email eq email.lowercase().trim() }
                .firstOrNull()?.let { row ->
                    UserResponse(
                        id = row[Users.id].value,
                        email = row[Users.email],
                        firstName = row[Users.firstName],
                        lastName = row[Users.lastName],
                        avatarUrl = row[Users.avatarUrl],
                        status = row[Users.status],
                        emailVerifiedAt = row[Users.emailVerifiedAt],
                        mfaEnabled = row[Users.mfaEnabled],
                        lastLoginAt = row[Users.lastLoginAt],
                        createdAt = row[Users.createdAt],
                        updatedAt = row[Users.updatedAt]
                    )
                }
        }
    }
    
    /**
     * Update user profile.
     */
    fun updateUser(
        userId: UUID,
        firstName: String? = null,
        lastName: String? = null,
        avatarUrl: String? = null,
        ipAddress: String? = null
    ): UserResponse? {
        return transaction {
            val updated = Users.update({ Users.id eq userId }) {
                firstName?.let { f -> it[Users.firstName] = f }
                lastName?.let { l -> it[Users.lastName] = l }
                avatarUrl?.let { a -> it[Users.avatarUrl] = a }
                it[updatedAt] = Instant.now()
            }
            
            if (updated > 0) {
                auditService.logUserAction(
                    actorUserId = userId,
                    action = "user_updated",
                    metadata = mapOf(
                        "first_name" to firstName,
                        "last_name" to lastName
                    ),
                    ipAddress = ipAddress
                )
                getUserResponse(userId)
            } else {
                null
            }
        }
    }
    
    /**
     * Change user password.
     */
    fun changePassword(
        userId: UUID,
        currentPassword: String,
        newPassword: String,
        ipAddress: String? = null
    ): Boolean {
        return transaction {
            val user = Users.select { Users.id eq userId }.firstOrNull()
                ?: return@transaction false
            
            // Verify current password
            if (!BCrypt.checkpw(currentPassword, user[Users.passwordHash])) {
                return@transaction false
            }
            
            // Hash new password
            val newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
            
            Users.update({ Users.id eq userId }) {
                it[passwordHash] = newHash
                it[updatedAt] = Instant.now()
            }
            
            auditService.logUserAction(
                actorUserId = userId,
                action = "password_changed",
                ipAddress = ipAddress
            )
            
            true
        }
    }
    
    /**
     * Delete user account (soft delete).
     */
    fun deleteUser(
        userId: UUID,
        password: String,
        ipAddress: String? = null
    ): Boolean {
        return transaction {
            val user = Users.select { Users.id eq userId }.firstOrNull()
                ?: return@transaction false
            
            // Verify password
            if (!BCrypt.checkpw(password, user[Users.passwordHash])) {
                return@transaction false
            }
            
            val now = Instant.now()
            
            // Soft delete
            Users.update({ Users.id eq userId }) {
                it[status] = "deleted"
                it[deletedAt] = now
                it[scheduledPurgeAt] = now.plus(30, ChronoUnit.DAYS)
                it[updatedAt] = now
            }
            
            // Revoke all business memberships
            BusinessMemberships.update({ BusinessMemberships.userId eq userId }) {
                it[status] = "revoked"
                it[revokedAt] = now
            }
            
            // Revoke all role assignments
            UserRoles.update({ UserRoles.userId eq userId }) {
                it[revokedAt] = now
            }
            
            auditService.logUserAction(
                actorUserId = userId,
                action = "user_deleted",
                ipAddress = ipAddress
            )
            
            true
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // AUTHENTICATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Authenticate user with email and password.
     * Handles account lockout after failed attempts.
     * 
     * Also supports login by username (for migrated accounts where email might be username@migrated.ground.dev)
     */
    fun authenticate(
        email: String,
        password: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ): AuthResult {
        return transaction {
            val normalizedInput = email.lowercase().trim()
            
            // First, try to find by exact email match
            var user = Users.select { Users.email eq normalizedInput }.firstOrNull()
            
            // If not found and input doesn't look like an email, try as username
            // (for migrated accounts where email is username@migrated.ground.dev)
            if (user == null && !normalizedInput.contains("@")) {
                val migratedEmail = "$normalizedInput@migrated.ground.dev"
                user = Users.select { Users.email eq migratedEmail }.firstOrNull()
            }
            
            // Also try matching by firstName (case-insensitive, set to username during migration)
            if (user == null) {
                user = Users.select { Users.firstName.lowerCase() eq normalizedInput }.firstOrNull()
            }
            
            if (user == null) return@transaction AuthResult.InvalidCredentials
            
            val userId = user[Users.id].value
            
            // Check if account is locked
            val lockedUntil = user[Users.lockedUntil]
            if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
                auditService.logUserAction(
                    actorUserId = userId,
                    action = "login_failed",
                    metadata = mapOf("reason" to "account_locked"),
                    ipAddress = ipAddress,
                    userAgent = userAgent
                )
                return@transaction AuthResult.AccountLocked(lockedUntil)
            }
            
            // Check if account is active
            if (user[Users.status] != "active") {
                auditService.logUserAction(
                    actorUserId = userId,
                    action = "login_failed",
                    metadata = mapOf("reason" to "account_inactive", "status" to user[Users.status]),
                    ipAddress = ipAddress,
                    userAgent = userAgent
                )
                return@transaction AuthResult.AccountInactive
            }
            
            // Verify password
            if (!BCrypt.checkpw(password, user[Users.passwordHash])) {
                // Increment failed attempts
                val attempts = user[Users.failedLoginAttempts] + 1
                
                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    // Lock account
                    val lockUntil = Instant.now().plus(LOCKOUT_DURATION_MINUTES, ChronoUnit.MINUTES)
                    Users.update({ Users.id eq userId }) {
                        it[failedLoginAttempts] = attempts
                        it[Users.lockedUntil] = lockUntil
                    }
                    
                    auditService.logUserAction(
                        actorUserId = userId,
                        action = "login_failed",
                        metadata = mapOf("reason" to "locked_after_max_attempts", "attempts" to attempts.toString()),
                        ipAddress = ipAddress,
                        userAgent = userAgent
                    )
                    
                    return@transaction AuthResult.AccountLocked(lockUntil)
                } else {
                    Users.update({ Users.id eq userId }) {
                        it[failedLoginAttempts] = attempts
                    }
                    
                    auditService.logUserAction(
                        actorUserId = userId,
                        action = "login_failed",
                        metadata = mapOf("reason" to "invalid_password", "attempts" to attempts.toString()),
                        ipAddress = ipAddress,
                        userAgent = userAgent
                    )
                    
                    return@transaction AuthResult.InvalidCredentials
                }
            }
            
            // Successful login - reset failed attempts
            val now = Instant.now()
            Users.update({ Users.id eq userId }) {
                it[failedLoginAttempts] = 0
                it[Users.lockedUntil] = null
                it[lastLoginAt] = now
                it[lastLoginIp] = ipAddress
            }
            
            auditService.logUserAction(
                actorUserId = userId,
                action = "login_success",
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            
            AuthResult.Success(getUserResponse(userId)!!)
        }
    }
    
    /**
     * Verify email address.
     */
    fun verifyEmail(
        userId: UUID,
        ipAddress: String? = null
    ): Boolean {
        return transaction {
            val updated = Users.update({ Users.id eq userId }) {
                it[emailVerifiedAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
            
            if (updated > 0) {
                auditService.logUserAction(
                    actorUserId = userId,
                    action = "email_verified",
                    ipAddress = ipAddress
                )
                true
            } else {
                false
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MFA
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Enable MFA for user.
     */
    fun enableMfa(
        userId: UUID,
        secret: String,
        ipAddress: String? = null
    ): Boolean {
        return transaction {
            Users.update({ Users.id eq userId }) {
                it[mfaEnabled] = true
                it[mfaSecret] = secret
                it[updatedAt] = Instant.now()
            }
            
            auditService.logUserAction(
                actorUserId = userId,
                action = "mfa_enabled",
                ipAddress = ipAddress
            )
            
            true
        }
    }
    
    /**
     * Disable MFA for user.
     */
    fun disableMfa(
        userId: UUID,
        ipAddress: String? = null
    ): Boolean {
        return transaction {
            Users.update({ Users.id eq userId }) {
                it[mfaEnabled] = false
                it[mfaSecret] = null
                it[updatedAt] = Instant.now()
            }
            
            auditService.logUserAction(
                actorUserId = userId,
                action = "mfa_disabled",
                ipAddress = ipAddress
            )
            
            true
        }
    }
    
    /**
     * Get MFA secret for verification.
     */
    fun getMfaSecret(userId: UUID): String? {
        return transaction {
            Users.select { Users.id eq userId }
                .firstOrNull()?.get(Users.mfaSecret)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════
    
    private fun getUserResponse(userId: UUID): UserResponse? {
        return Users.select { Users.id eq userId }.firstOrNull()?.let { row ->
            UserResponse(
                id = row[Users.id].value,
                email = row[Users.email],
                firstName = row[Users.firstName],
                lastName = row[Users.lastName],
                avatarUrl = row[Users.avatarUrl],
                status = row[Users.status],
                emailVerifiedAt = row[Users.emailVerifiedAt],
                mfaEnabled = row[Users.mfaEnabled],
                lastLoginAt = row[Users.lastLoginAt],
                createdAt = row[Users.createdAt],
                updatedAt = row[Users.updatedAt]
            )
        }
    }
    
    /**
     * Get internal user data (including password hash) for authentication.
     * This should only be used internally.
     */
    internal fun getInternalUser(userId: UUID): InternalUser? {
        return transaction {
            Users.select { Users.id eq userId }.firstOrNull()?.let { row ->
                InternalUser(
                    id = row[Users.id].value,
                    email = row[Users.email],
                    passwordHash = row[Users.passwordHash],
                    status = row[Users.status],
                    mfaEnabled = row[Users.mfaEnabled],
                    mfaSecret = row[Users.mfaSecret]
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════

data class UserResponse(
    val id: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val status: String,
    val emailVerifiedAt: Instant?,
    val mfaEnabled: Boolean,
    val lastLoginAt: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class InternalUser(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val status: String,
    val mfaEnabled: Boolean,
    val mfaSecret: String?
)

sealed class AuthResult {
    data class Success(val user: UserResponse) : AuthResult()
    object InvalidCredentials : AuthResult()
    data class AccountLocked(val until: Instant) : AuthResult()
    object AccountInactive : AuthResult()
    object MfaRequired : AuthResult()
}

