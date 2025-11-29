package com.tbd.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Transactions : UUIDTable("transactions") {
    val accountId = uuid("account_id").references(Accounts.id)
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
