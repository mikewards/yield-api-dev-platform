package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.Accounts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class AccountService {
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
            
            // Validate password
            if (request.password.length < 8) {
                throw IllegalArgumentException("Password must be at least 8 characters")
            }
            
            // Hash password
            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            
            // Create account
            val now = java.time.Instant.now()
            val accountId = Accounts.insert {
                it[Accounts.username] = request.username
                it[Accounts.passwordHash] = passwordHash
                it[Accounts.email] = request.email
                it[Accounts.status] = "active"
                it[Accounts.createdAt] = now
                it[Accounts.updatedAt] = now
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
    
    fun authenticate(request: AuthenticateRequest): AuthenticateResponse {
        return transaction {
            val account = Accounts.select { Accounts.username eq request.username }.firstOrNull()
                ?: throw IllegalArgumentException("Invalid username or password")
            
            val isValid = BCrypt.checkpw(request.password, account[Accounts.passwordHash])
            if (!isValid) {
                throw IllegalArgumentException("Invalid username or password")
            }
            
            val accountId = account[Accounts.id].value
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
}
