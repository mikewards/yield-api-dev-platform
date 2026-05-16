package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Positions : UUIDTable("positions") {
    val accountId = uuid("account_id").references(Accounts.id)
    val yieldAccountId = uuid("yield_account_id").references(YieldAccounts.id)
    val currency = varchar("currency", 10)
    val protocol = varchar("protocol", 20)
    val principal = decimal("principal", 20, 8)
    val accruedYield = decimal("accrued_yield", 20, 8).default(0.toBigDecimal())
    val annualYieldRate = decimal("annual_yield_rate", 5, 4)
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}
