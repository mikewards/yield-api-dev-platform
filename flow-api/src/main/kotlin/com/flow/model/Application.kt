package com.flow.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Applications : UUIDTable("applications") {
    val accountId = uuid("account_id").references(Accounts.id)
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

