package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Webhooks - Autonomous resource (RCAC model)
 * 
 * Webhooks do NOT belong to businesses/accounts.
 * Access is controlled via ResourceAccess table.
 */
object Webhooks : UUIDTable("webhooks") {
    // DEPRECATED: accountId is kept for backward compatibility during migration
    val accountId = uuid("account_id").references(Accounts.id).nullable()
    
    // RCAC: Who created this resource (for audit trail, NOT ownership)
    val createdBy = uuid("created_by").references(Users.id).nullable()
    
    val url = varchar("url", 500)
    val events = text("events") // JSON array of event types
    val secret = varchar("secret", 255).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}
