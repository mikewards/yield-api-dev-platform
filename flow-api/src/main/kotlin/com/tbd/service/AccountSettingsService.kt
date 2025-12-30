package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * AccountSettingsService handles account settings, sessions, and soft delete.
 * 
 * Soft Delete:
 * - Accounts are marked as "deleted" with anonymized PII
 * - Scheduled for permanent deletion after 30 days
 * - Data can be recovered within the 30-day window (admin only)
 */
class AccountSettingsService {
    
    companion object {
        // Soft delete retention period: 30 days
        const val RETENTION_DAYS = 30L
    }
    
    private val tokenService = TokenService()
    
    /**
     * Get account settings for display (excludes deleted accounts)
     */
    fun getAccountSettings(accountId: UUID): AccountSettingsResponse? {
        return transaction {
            Accounts.select { 
                (Accounts.id eq accountId) and 
                (Accounts.status neq "deleted")
            }
                .firstOrNull()
                ?.let { row ->
                    AccountSettingsResponse(
                        account_id = row[Accounts.id].value.toString(),
                        username = row[Accounts.username],
                        email = row[Accounts.email],
                        created_at = row[Accounts.createdAt]?.toString() ?: "",
                        status = row[Accounts.status]
                    )
                }
        }
    }
    
    /**
     * Update email address (requires password verification)
     */
    fun updateEmail(accountId: UUID, request: UpdateEmailRequest): UpdateEmailResponse {
        return transaction {
            // Get current account (exclude deleted)
            val account = Accounts.select { 
                (Accounts.id eq accountId) and 
                (Accounts.status neq "deleted")
            }.firstOrNull()
                ?: throw IllegalArgumentException("Account not found")
            
            // Verify current password
            val isValid = BCrypt.checkpw(request.password, account[Accounts.passwordHash])
            if (!isValid) {
                throw IllegalArgumentException("Invalid password")
            }
            
            // Validate email format
            if (!request.email.contains("@") || !request.email.contains(".")) {
                throw IllegalArgumentException("Invalid email format")
            }
            
            // Check if email is already in use by another active account
            val existingEmail = Accounts.select { 
                (Accounts.email eq request.email) and 
                (Accounts.id neq accountId) and
                (Accounts.status neq "deleted")
            }.firstOrNull()
            
            if (existingEmail != null) {
                throw IllegalArgumentException("Email is already in use")
            }
            
            // Update email
            Accounts.update({ Accounts.id eq accountId }) {
                it[Accounts.email] = request.email
                it[Accounts.updatedAt] = Instant.now()
            }
            
            UpdateEmailResponse(
                success = true,
                email = request.email,
                message = "Email updated successfully"
            )
        }
    }
    
    /**
     * Change password (requires current password verification)
     */
    fun changePassword(accountId: UUID, request: ChangePasswordRequest): ChangePasswordResponse {
        return transaction {
            // Get current account (exclude deleted)
            val account = Accounts.select { 
                (Accounts.id eq accountId) and 
                (Accounts.status neq "deleted")
            }.firstOrNull()
                ?: throw IllegalArgumentException("Account not found")
            
            // Verify current password
            val isValid = BCrypt.checkpw(request.current_password, account[Accounts.passwordHash])
            if (!isValid) {
                throw IllegalArgumentException("Current password is incorrect")
            }
            
            // Validate new password
            if (request.new_password.length < 8) {
                throw IllegalArgumentException("New password must be at least 8 characters")
            }
            
            // Check that new password is different
            if (request.current_password == request.new_password) {
                throw IllegalArgumentException("New password must be different from current password")
            }
            
            // Hash and update password
            val newPasswordHash = BCrypt.hashpw(request.new_password, BCrypt.gensalt())
            Accounts.update({ Accounts.id eq accountId }) {
                it[Accounts.passwordHash] = newPasswordHash
                it[Accounts.updatedAt] = Instant.now()
            }
            
            // SECURITY: Revoke ALL refresh tokens (force re-login on all devices)
            // This is critical - if password was compromised, attacker's sessions must be invalidated
            tokenService.revokeAllRefreshTokens(accountId)
            
            // Also revoke all sessions
            val now = Instant.now()
            UserSessions.update({ UserSessions.accountId eq accountId }) {
                it[UserSessions.revokedAt] = now
            }
            
            println("🔐 Password changed and all sessions revoked for account: $accountId")
            
            ChangePasswordResponse(
                success = true,
                message = "Password changed successfully. Please sign in again on all devices."
            )
        }
    }
    
    /**
     * Get all active sessions for an account
     */
    fun getSessions(accountId: UUID, currentTokenHash: String?): SessionsListResponse {
        return transaction {
            val sessions = UserSessions.select {
                (UserSessions.accountId eq accountId) and
                (UserSessions.revokedAt.isNull())
            }.orderBy(UserSessions.lastActiveAt, SortOrder.DESC)
            .map { row ->
                SessionResponse(
                    session_id = row[UserSessions.id].value.toString(),
                    ip_address = row[UserSessions.ipAddress],
                    device_info = row[UserSessions.deviceInfo],
                    last_active_at = row[UserSessions.lastActiveAt].toString(),
                    created_at = row[UserSessions.createdAt].toString(),
                    is_current = false // Will be updated if we have current token info
                )
            }
            
            SessionsListResponse(
                sessions = sessions,
                total = sessions.size
            )
        }
    }
    
    /**
     * Create a new session when user logs in
     */
    fun createSession(
        accountId: UUID, 
        refreshTokenId: UUID,
        ipAddress: String?,
        userAgent: String?
    ): UUID {
        return transaction {
            val now = Instant.now()
            val deviceInfo = parseUserAgent(userAgent)
            
            val sessionId = UserSessions.insert {
                it[UserSessions.accountId] = accountId
                it[UserSessions.refreshTokenId] = refreshTokenId
                it[UserSessions.ipAddress] = ipAddress
                it[UserSessions.userAgent] = userAgent
                it[UserSessions.deviceInfo] = deviceInfo
                it[UserSessions.lastActiveAt] = now
                it[UserSessions.createdAt] = now
            } get UserSessions.id
            
            sessionId.value
        }
    }
    
    /**
     * Revoke a specific session
     */
    fun revokeSession(accountId: UUID, sessionId: UUID): RevokeSessionResponse {
        return transaction {
            val now = Instant.now()
            
            // Find the session and verify ownership
            val session = UserSessions.select {
                (UserSessions.id eq sessionId) and
                (UserSessions.accountId eq accountId) and
                (UserSessions.revokedAt.isNull())
            }.firstOrNull() ?: throw IllegalArgumentException("Session not found")
            
            // Revoke the session
            UserSessions.update({ UserSessions.id eq sessionId }) {
                it[UserSessions.revokedAt] = now
            }
            
            // Also revoke the associated refresh token
            val refreshTokenId = session[UserSessions.refreshTokenId]
            if (refreshTokenId != null) {
                RefreshTokens.update({ RefreshTokens.id eq refreshTokenId }) {
                    it[RefreshTokens.revokedAt] = now
                }
            }
            
            RevokeSessionResponse(
                success = true,
                message = "Session revoked successfully"
            )
        }
    }
    
    /**
     * Revoke all sessions except current
     */
    fun revokeAllOtherSessions(accountId: UUID, currentSessionId: UUID?): RevokeAllSessionsResponse {
        return transaction {
            val now = Instant.now()
            
            // Get all active sessions except current
            val sessionsToRevoke = UserSessions.select {
                (UserSessions.accountId eq accountId) and
                (UserSessions.revokedAt.isNull()) and
                (if (currentSessionId != null) UserSessions.id neq currentSessionId else Op.TRUE)
            }.toList()
            
            var revokedCount = 0
            
            sessionsToRevoke.forEach { session ->
                // Revoke the session
                UserSessions.update({ UserSessions.id eq session[UserSessions.id] }) {
                    it[UserSessions.revokedAt] = now
                }
                
                // Revoke the associated refresh token
                val refreshTokenId = session[UserSessions.refreshTokenId]
                if (refreshTokenId != null) {
                    RefreshTokens.update({ RefreshTokens.id eq refreshTokenId }) {
                        it[RefreshTokens.revokedAt] = now
                    }
                }
                
                revokedCount++
            }
            
            RevokeAllSessionsResponse(
                success = true,
                revoked_count = revokedCount,
                message = "Revoked $revokedCount other session(s)"
            )
        }
    }
    
    /**
     * Soft delete account with 30-day retention.
     * 
     * This function:
     * 1. Verifies password and confirmation
     * 2. Revokes all sessions and tokens
     * 3. Anonymizes PII (username, email)
     * 4. Sets status to "deleted"
     * 5. Schedules permanent deletion after 30 days
     * 
     * User data is NOT immediately deleted - it's retained for 30 days to allow:
     * - Account recovery if deleted accidentally
     * - Fraud investigation if needed
     * - Compliance with data retention requirements
     */
    fun deleteAccount(accountId: UUID, request: DeleteAccountRequest): DeleteAccountResponse {
        return transaction {
            // Verify confirmation text
            if (request.confirmation.uppercase() != "DELETE") {
                throw IllegalArgumentException("Please type DELETE to confirm account deletion")
            }
            
            // Get current account (exclude already deleted)
            val account = Accounts.select { 
                (Accounts.id eq accountId) and 
                (Accounts.status neq "deleted")
            }.firstOrNull()
                ?: throw IllegalArgumentException("Account not found")
            
            // Verify password
            val isValid = BCrypt.checkpw(request.password, account[Accounts.passwordHash])
            if (!isValid) {
                throw IllegalArgumentException("Invalid password")
            }
            
            val now = Instant.now()
            val purgeAt = now.plus(RETENTION_DAYS, ChronoUnit.DAYS)
            
            // 1. Revoke all sessions
            UserSessions.update({ UserSessions.accountId eq accountId }) {
                it[UserSessions.revokedAt] = now
            }
            
            // 2. Revoke all refresh tokens
            RefreshTokens.update({ RefreshTokens.accountId eq accountId }) {
                it[RefreshTokens.revokedAt] = now
            }
            
            // 3. Soft delete the account - anonymize PII and mark as deleted
            val anonymizedUsername = "deleted_${accountId.toString().take(8)}"
            
            Accounts.update({ Accounts.id eq accountId }) {
                it[Accounts.status] = "deleted"
                it[Accounts.username] = anonymizedUsername  // Anonymize username
                it[Accounts.email] = null                    // Remove email
                it[Accounts.deletedAt] = now
                it[Accounts.scheduledPurgeAt] = purgeAt
                it[Accounts.updatedAt] = now
            }
            
            println("🗑️ Account soft deleted: $accountId, scheduled purge: $purgeAt")
            
            DeleteAccountResponse(
                success = true,
                message = "Account scheduled for deletion. Your data will be permanently removed in $RETENTION_DAYS days."
            )
        }
    }
    
    /**
     * Parse user agent to get a friendly device name
     */
    private fun parseUserAgent(userAgent: String?): String? {
        if (userAgent == null) return null
        
        return when {
            userAgent.contains("iPhone") -> "iPhone"
            userAgent.contains("iPad") -> "iPad"
            userAgent.contains("Android") -> "Android Device"
            userAgent.contains("Windows") -> "Windows PC"
            userAgent.contains("Mac OS") || userAgent.contains("Macintosh") -> "Mac"
            userAgent.contains("Linux") -> "Linux PC"
            userAgent.contains("Chrome") -> "Chrome Browser"
            userAgent.contains("Firefox") -> "Firefox Browser"
            userAgent.contains("Safari") -> "Safari Browser"
            else -> "Unknown Device"
        }
    }
}
