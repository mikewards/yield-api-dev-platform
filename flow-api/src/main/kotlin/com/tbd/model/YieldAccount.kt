package com.tbd.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object YieldAccounts : UUIDTable("yield_accounts") {
    val accountId = uuid("account_id").references(Accounts.id)
    val currency = varchar("currency", 10)
    val protocol = varchar("protocol", 20).default("auto")
    val annualYieldRate = decimal("annual_yield_rate", 5, 4).default(0.06.toBigDecimal())
    val status = varchar("status", 20).default("active")
    val balance = decimal("balance", 20, 8).default(0.toBigDecimal())
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}
