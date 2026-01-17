package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Transactions - Autonomous resource (RCAC model)
 * 
 * Transactions do NOT belong to businesses/accounts.
 * Access is controlled via ResourceAccess table.
 * 
 * Transactions are typically accessed via their parent YieldAccount.
 * If you have access to a YieldAccount, you can see its transactions.
 */
object Transactions : UUIDTable("transactions") {
    // DEPRECATED: accountId is kept for backward compatibility during migration
    val accountId = uuid("account_id").references(Accounts.id).nullable()
    
    // RCAC: Who created this resource (for audit trail, NOT ownership)
    val createdBy = uuid("created_by").references(Users.id).nullable()
    
    val yieldAccountId = uuid("yield_account_id").references(YieldAccounts.id).nullable()
    val type = varchar("type", 20) // deposit, withdraw, yield_accrual
    val status = varchar("status", 20).default("pending") // pending, completed, failed
    val amount = decimal("amount", 20, 8)
    val currency = varchar("currency", 10)
    val destinationAddress = varchar("destination_address", 255).nullable()
    val sourceAddress = varchar("source_address", 255).nullable()
    val createdAt = timestamp("created_at").nullable()
    val completedAt = timestamp("completed_at").nullable()
}
