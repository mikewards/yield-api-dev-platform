package com.ground.service

import com.ground.model.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

/**
 * Background job to permanently delete accounts that have been soft-deleted
 * and have passed their retention period (30 days).
 * 
 * This job runs every 6 hours and:
 * 1. Finds accounts where scheduledPurgeAt < now
 * 2. Hard deletes all associated data
 * 3. Hard deletes the account record
 */
object AccountCleanupJob {
    private var job: Job? = null
    
    // Run every 6 hours
    private const val INTERVAL_MS = 6 * 60 * 60 * 1000L
    
    fun start() {
        job = CoroutineScope(Dispatchers.Default).launch {
            println("🗑️ Account cleanup job started (runs every 6 hours)")
            while (isActive) {
                try {
                    purgeExpiredAccounts()
                } catch (e: Exception) {
                    println("❌ Account cleanup job error: ${e.message}")
                    e.printStackTrace()
                }
                delay(INTERVAL_MS)
            }
        }
    }
    
    fun stop() {
        job?.cancel()
        println("🛑 Account cleanup job stopped")
    }
    
    /**
     * Find and permanently delete all accounts past their retention period.
     */
    fun purgeExpiredAccounts(): Int {
        return transaction {
            val now = Instant.now()
            
            // Find accounts ready for permanent deletion
            val expiredAccounts = Accounts.select {
                (Accounts.status eq "deleted") and
                (Accounts.scheduledPurgeAt.isNotNull()) and
                (Accounts.scheduledPurgeAt lessEq now)
            }.map { it[Accounts.id].value }
            
            if (expiredAccounts.isEmpty()) {
                return@transaction 0
            }
            
            println("🗑️ Purging ${expiredAccounts.size} expired account(s)...")
            
            expiredAccounts.forEach { accountId ->
                hardDeleteAccount(accountId)
            }
            
            println("✅ Purged ${expiredAccounts.size} expired account(s)")
            expiredAccounts.size
        }
    }
    
    /**
     * Permanently delete an account and all associated data.
     * This is the "hard delete" that happens after the retention period.
     */
    private fun hardDeleteAccount(accountId: java.util.UUID) {
        transaction {
            println("🗑️ Hard deleting account: $accountId")
            
            // 1. Delete request logs
            val logsDeleted = RequestLogs.deleteWhere { RequestLogs.accountId eq accountId }
            
            // 2. Delete access tokens (PATs)
            val tokensDeleted = AccessTokens.deleteWhere { AccessTokens.accountId eq accountId }
            
            // 3. Delete application wallets (for each application)
            val appIds = Applications.select { Applications.accountId eq accountId }
                .map { it[Applications.id].value }
            
            var walletsDeleted = 0
            appIds.forEach { appId ->
                walletsDeleted += ApplicationWallets.deleteWhere { ApplicationWallets.applicationId eq appId }
            }
            
            // 4. Delete transactions and positions for yield accounts
            val yieldAccountIds = YieldAccounts.select { YieldAccounts.accountId eq accountId }
                .map { it[YieldAccounts.id].value }
            
            var transactionsDeleted = 0
            var positionsDeleted = 0
            yieldAccountIds.forEach { yaId ->
                transactionsDeleted += Transactions.deleteWhere { Transactions.yieldAccountId eq yaId }
                positionsDeleted += Positions.deleteWhere { Positions.yieldAccountId eq yaId }
            }
            
            // 5. Delete yield accounts
            val yieldAccountsDeleted = YieldAccounts.deleteWhere { YieldAccounts.accountId eq accountId }
            
            // 6. Delete applications
            val applicationsDeleted = Applications.deleteWhere { Applications.accountId eq accountId }
            
            // 7. Delete webhooks
            val webhooksDeleted = Webhooks.deleteWhere { Webhooks.accountId eq accountId }
            
            // 8. Delete refresh tokens
            val refreshTokensDeleted = RefreshTokens.deleteWhere { RefreshTokens.accountId eq accountId }
            
            // 9. Delete sessions
            val sessionsDeleted = UserSessions.deleteWhere { UserSessions.accountId eq accountId }
            
            // 10. Finally, delete the account
            val accountDeleted = Accounts.deleteWhere { Accounts.id eq accountId }
            
            println("   ✓ Deleted: account=$accountDeleted, apps=$applicationsDeleted, " +
                    "yieldAccounts=$yieldAccountsDeleted, tokens=$tokensDeleted, " +
                    "sessions=$sessionsDeleted, logs=$logsDeleted")
        }
    }
}

