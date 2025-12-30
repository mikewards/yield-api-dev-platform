package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.Accounts
import com.tbd.model.UserSessions
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.*

/**
 * AccountService handles account creation and authentication with security features:
 * - BCrypt password hashing
 * - Account lockout after failed attempts (5 attempts → 15 minute lockout)
 * - Password complexity validation
 * - Session creation on login
 */
class AccountService {
    
    companion object {
        // Security constants
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15L
    }
    
    fun createAccount(request: CreateAccountRequest): AccountResponse {
        return transaction {
            // Check if username exists (only active accounts)
            val existing = Accounts.select { 
                (Accounts.username eq request.username) and 
                (Accounts.status neq "deleted")
            }.firstOrNull()
            if (existing != null) {
                throw IllegalArgumentException("Username is already taken")
            }
            
            // Validate username
            if (request.username.length < 3 || request.username.length > 50) {
                throw IllegalArgumentException("Username must be between 3 and 50 characters")
            }
            if (!request.username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
                throw IllegalArgumentException("Username can only contain letters, numbers, and underscores")
            }
            
            // Validate password with stronger requirements
            validatePassword(request.password)
            
            // Hash password
            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            
            // Create account
            val now = Instant.now()
            val accountId = Accounts.insert {
                it[Accounts.username] = request.username
                it[Accounts.passwordHash] = passwordHash
                it[Accounts.email] = request.email
                it[Accounts.status] = "active"
                it[Accounts.createdAt] = now
                it[Accounts.updatedAt] = now
                it[Accounts.failedLoginAttempts] = 0
                it[Accounts.lockedUntil] = null
            } get Accounts.id
            
            val account = Accounts.select { Accounts.id eq accountId }.first()
            AccountResponse(
                account_id = accountId.value.toString(),
                username = request.username,
                created_at = account[Accounts.createdAt]?.toString() ?: now.toString(),
                status = "active"
            )
        }
    }
    
    /**
     * Authenticate a user with account lockout protection and session creation.
     * 
     * Security:
     * - After 5 failed attempts, account is locked for 15 minutes
     * - Failed attempt counter resets on successful login
     * - Lockout is time-based and auto-expires
     * - Session is created and linked to refresh token
     * 
     * @param request Authentication request with username and password
     * @param ipAddress Client IP address for session tracking
     * @param userAgent Client user agent for device identification
     */
    fun authenticate(request: AuthenticateRequest, ipAddress: String? = null, userAgent: String? = null): AuthenticateResponse {
        return transaction {
            val account = Accounts.select { 
                (Accounts.username eq request.username) and
                (Accounts.status neq "deleted")
            }.firstOrNull()
                ?: throw IllegalArgumentException("Invalid username or password")
            
            val accountId = account[Accounts.id].value
            val now = Instant.now()
            
            // Check if account is locked
            val lockedUntil = account[Accounts.lockedUntil]
            if (lockedUntil != null && now.isBefore(lockedUntil)) {
                val remainingMinutes = java.time.Duration.between(now, lockedUntil).toMinutes() + 1
                println("🔒 Account locked: $accountId until $lockedUntil")
                throw AccountLockedException("Account is temporarily locked. Try again in $remainingMinutes minute(s).")
            }
            
            // If lockout has expired, reset the counter
            if (lockedUntil != null && now.isAfter(lockedUntil)) {
                Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                    it[Accounts.failedLoginAttempts] = 0
                    it[Accounts.lockedUntil] = null
                }
            }
            
            // Validate password
            val isValid = BCrypt.checkpw(request.password, account[Accounts.passwordHash])
            
            if (!isValid) {
                // Increment failed attempts
                val currentAttempts = account[Accounts.failedLoginAttempts] ?: 0
                val newAttempts = currentAttempts + 1
                
                if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                    // Lock the account
                    val lockUntil = now.plusSeconds(LOCKOUT_DURATION_MINUTES * 60)
                    Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                        it[Accounts.failedLoginAttempts] = newAttempts
                        it[Accounts.lockedUntil] = lockUntil
                    }
                    println("🔒 Account locked after $newAttempts failed attempts: $accountId")
                    throw AccountLockedException("Too many failed login attempts. Account locked for $LOCKOUT_DURATION_MINUTES minutes.")
                } else {
                    Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                        it[Accounts.failedLoginAttempts] = newAttempts
                    }
                    val remaining = MAX_FAILED_ATTEMPTS - newAttempts
                    println("⚠️ Failed login attempt $newAttempts/$MAX_FAILED_ATTEMPTS for account: $accountId")
                    throw IllegalArgumentException("Invalid username or password. $remaining attempt(s) remaining.")
                }
            }
            
            // Successful login - reset failed attempts
            Accounts.update({ Accounts.id eq account[Accounts.id] }) {
                it[Accounts.failedLoginAttempts] = 0
                it[Accounts.lockedUntil] = null
                it[Accounts.updatedAt] = now
            }
            
            val tokenService = TokenService()
            
            // Generate refresh token first (we need its ID for the session)
            val refreshResult = tokenService.generateRefreshToken(accountId)
            
            // Create session linked to the refresh token
            val sessionId = createSession(
                accountId = accountId,
                refreshTokenId = refreshResult.tokenId,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            
            // Generate access token with embedded session ID
            val accessToken = tokenService.generateAccessToken(accountId, sessionId)
            
            println("🔐 User authenticated: $accountId (session: $sessionId)")
            
            AuthenticateResponse(
                access_token = accessToken,
                refresh_token = refreshResult.token,
                token_type = "Bearer",
                expires_in = TokenService.ACCESS_TOKEN_LIFETIME_SEC,
                account_id = accountId.toString()
            )
        }
    }
    
    /**
     * Create a session record for tracking active logins.
     */
    private fun createSession(
        accountId: UUID,
        refreshTokenId: UUID,
        ipAddress: String?,
        userAgent: String?
    ): UUID {
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
        
        println("📱 Session created: ${sessionId.value} for account: $accountId (device: $deviceInfo)")
        return sessionId.value
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
    
    fun getAccount(accountId: UUID): AccountResponse? {
        return transaction {
            Accounts.select { 
                (Accounts.id eq accountId) and 
                (Accounts.status neq "deleted")
            }.firstOrNull()?.let {
                AccountResponse(
                    account_id = it[Accounts.id].value.toString(),
                    username = it[Accounts.username],
                    created_at = it[Accounts.createdAt]?.toString() ?: "",
                    status = it[Accounts.status]
                )
            }
        }
    }
    
    /**
     * Validate password strength.
     * Requirements:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one number
     */
    private fun validatePassword(password: String) {
        if (password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            throw IllegalArgumentException("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            throw IllegalArgumentException("Password must contain at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            throw IllegalArgumentException("Password must contain at least one number")
        }
    }
}

/**
 * Exception thrown when an account is locked due to too many failed login attempts.
 */
class AccountLockedException(message: String) : Exception(message)
