package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.*

class AccountSettingsService {
    
    private val tokenService = TokenService()
    
    /**
     * Get account settings for display
     */
    fun getAccountSettings(accountId: UUID): AccountSettingsResponse? {
        return transaction {
            Accounts.select { Accounts.id eq accountId }
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
            // Get current account
            val account = Accounts.select { Accounts.id eq accountId }.firstOrNull()
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
            
            // Check if email is already in use by another account
            val existingEmail = Accounts.select { 
                (Accounts.email eq request.email) and (Accounts.id neq accountId)
            }.firstOrNull()
            
            if (existingEmail != null) {
                throw IllegalArgumentException("Email is already in use")
            }
            
            // Update email
            Accounts.update({ Accounts.id eq accountId }) {
                it[email] = request.email
                it[updatedAt] = Instant.now()
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
            // Get current account
            val account = Accounts.select { Accounts.id eq accountId }.firstOrNull()
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
                it[passwordHash] = newPasswordHash
                it[updatedAt] = Instant.now()
            }
            
            // Optionally: Revoke all refresh tokens except current (force re-login on other devices)
            // For now, we'll leave other sessions active
            
            ChangePasswordResponse(
                success = true,
                message = "Password changed successfully"
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
                it[revokedAt] = now
            }
            
            // Also revoke the associated refresh token
            val refreshTokenId = session[UserSessions.refreshTokenId]
            if (refreshTokenId != null) {
                RefreshTokens.update({ RefreshTokens.id eq refreshTokenId }) {
                    it[revokedAt] = now
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
                    it[revokedAt] = now
                }
                
                // Revoke the associated refresh token
                val refreshTokenId = session[UserSessions.refreshTokenId]
                if (refreshTokenId != null) {
                    RefreshTokens.update({ RefreshTokens.id eq refreshTokenId }) {
                        it[revokedAt] = now
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
     * Delete account (requires password verification and confirmation)
     */
    fun deleteAccount(accountId: UUID, request: DeleteAccountRequest): DeleteAccountResponse {
        return transaction {
            // Verify confirmation text
            if (request.confirmation.uppercase() != "DELETE") {
                throw IllegalArgumentException("Please type DELETE to confirm account deletion")
            }
            
            // Get current account
            val account = Accounts.select { Accounts.id eq accountId }.firstOrNull()
                ?: throw IllegalArgumentException("Account not found")
            
            // Verify password
            val isValid = BCrypt.checkpw(request.password, account[Accounts.passwordHash])
            if (!isValid) {
                throw IllegalArgumentException("Invalid password")
            }
            
            // Delete in order (to respect foreign key constraints)
            // 1. Revoke all sessions
            UserSessions.update({ UserSessions.accountId eq accountId }) {
                it[revokedAt] = Instant.now()
            }
            
            // 2. Revoke all refresh tokens
            RefreshTokens.update({ RefreshTokens.accountId eq accountId }) {
                it[revokedAt] = Instant.now()
            }
            
            // 3. Delete request logs
            RequestLogs.deleteWhere { RequestLogs.accountId eq accountId }
            
            // 4. Delete access tokens
            AccessTokens.deleteWhere { AccessTokens.accountId eq accountId }
            
            // 5. Delete application wallets (for each application)
            val appIds = Applications.select { Applications.accountId eq accountId }
                .map { it[Applications.id].value }
            
            appIds.forEach { appId ->
                ApplicationWallets.deleteWhere { ApplicationWallets.applicationId eq appId }
            }
            
            // 6. Delete transactions and positions for yield accounts
            val yieldAccountIds = YieldAccounts.select { YieldAccounts.accountId eq accountId }
                .map { it[YieldAccounts.id].value }
            
            yieldAccountIds.forEach { yaId ->
                Transactions.deleteWhere { Transactions.yieldAccountId eq yaId }
                Positions.deleteWhere { Positions.yieldAccountId eq yaId }
            }
            
            // 7. Delete yield accounts
            YieldAccounts.deleteWhere { YieldAccounts.accountId eq accountId }
            
            // 8. Delete applications
            Applications.deleteWhere { Applications.accountId eq accountId }
            
            // 9. Delete webhooks
            Webhooks.deleteWhere { Webhooks.accountId eq accountId }
            
            // 10. Delete refresh tokens (hard delete after soft delete)
            RefreshTokens.deleteWhere { RefreshTokens.accountId eq accountId }
            
            // 11. Delete sessions (hard delete after soft delete)
            UserSessions.deleteWhere { UserSessions.accountId eq accountId }
            
            // 12. Finally, delete the account
            Accounts.deleteWhere { Accounts.id eq accountId }
            
            DeleteAccountResponse(
                success = true,
                message = "Account deleted successfully"
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

