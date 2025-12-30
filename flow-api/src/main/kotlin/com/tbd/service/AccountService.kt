package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.Accounts
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
 */
class AccountService {
    
    companion object {
        // Security constants
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 15L
    }
    
    fun createAccount(request: CreateAccountRequest): AccountResponse {
        return transaction {
            // Check if username exists
            val existing = Accounts.select { Accounts.username eq request.username }.firstOrNull()
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
     * Authenticate a user with account lockout protection.
     * 
     * Security:
     * - After 5 failed attempts, account is locked for 15 minutes
     * - Failed attempt counter resets on successful login
     * - Lockout is time-based and auto-expires
     */
    fun authenticate(request: AuthenticateRequest): AuthenticateResponse {
        return transaction {
            val account = Accounts.select { Accounts.username eq request.username }.firstOrNull()
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
            
            // Generate short-lived access token (15 min) and long-lived refresh token (30 days)
            val accessToken = tokenService.generateAccessToken(accountId)
            val refreshToken = tokenService.generateRefreshToken(accountId)
            
            println("🔐 User authenticated: $accountId")
            
            AuthenticateResponse(
                access_token = accessToken,
                refresh_token = refreshToken,
                token_type = "Bearer",
                expires_in = TokenService.ACCESS_TOKEN_LIFETIME_SEC,
                account_id = accountId.toString()
            )
        }
    }
    
    fun getAccount(accountId: UUID): AccountResponse? {
        return transaction {
            Accounts.select { Accounts.id eq accountId }.firstOrNull()?.let {
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
