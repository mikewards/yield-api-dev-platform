package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Applications - Autonomous resource (RCAC model)
 * 
 * Applications do NOT belong to businesses/accounts.
 * Access is controlled via ResourceAccess table.
 * 
 * accountId is kept nullable for backward compatibility during migration.
 * createdBy tracks who created the resource (for audit, not ownership).
 */
object Applications : UUIDTable("applications") {
    // DEPRECATED: accountId is kept for backward compatibility during migration
    // Will be removed after migration to RCAC is complete
    val accountId = uuid("account_id").references(Accounts.id).nullable()
    
    // RCAC: Who created this resource (for audit trail, NOT ownership)
    // Access is controlled via ResourceAccess table, not this field
    val createdBy = uuid("created_by").references(Users.id).nullable()
    
    val name = varchar("name", 100)
    val description = varchar("description", 500).nullable()
    val environment = varchar("environment", 20).default("sandbox") // sandbox, production
    val status = varchar("status", 20).default("active") // active, inactive, suspended
    val webhookUrl = varchar("webhook_url", 500).nullable()
    val webhookSecret = varchar("webhook_secret", 100).nullable()
    val allowedOrigins = text("allowed_origins").nullable() // JSON array of allowed origins
    val permissions = text("permissions").nullable() // JSON array of permissions
    val sandboxRpcUrl = varchar("sandbox_rpc_url", 500).nullable()
    val productionRpcUrl = varchar("production_rpc_url", 500).nullable()
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}

