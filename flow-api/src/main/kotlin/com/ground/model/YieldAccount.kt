package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * YieldAccounts - Autonomous resource (RCAC model)
 * 
 * YieldAccounts do NOT belong to businesses/accounts.
 * Access is controlled via ResourceAccess table.
 */
object YieldAccounts : UUIDTable("yield_accounts") {
    // DEPRECATED: accountId is kept for backward compatibility during migration
    val accountId = uuid("account_id").references(Accounts.id).nullable()
    
    // RCAC: Who created this resource (for audit trail, NOT ownership)
    val createdBy = uuid("created_by").references(Users.id).nullable()
    
    // Optional: Link to application that created it (for API key context)
    val applicationId = uuid("application_id").references(Applications.id).nullable()
    
    val currency = varchar("currency", 10)
    val protocol = varchar("protocol", 20).default("auto")
    val annualYieldRate = decimal("annual_yield_rate", 5, 4).default(0.06.toBigDecimal())
    val status = varchar("status", 20).default("active")
    val balance = decimal("balance", 20, 8).default(0.toBigDecimal())
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}
