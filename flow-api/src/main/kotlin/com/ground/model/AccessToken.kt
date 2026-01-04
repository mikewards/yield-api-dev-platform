package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object AccessTokens : UUIDTable("access_tokens") {
    val accountId = uuid("account_id").references(Accounts.id)
    val applicationId = uuid("application_id").references(Applications.id).nullable()
    val token = varchar("token", 255).uniqueIndex()
    val name = varchar("name", 100)
    val environment = varchar("environment", 20).default("sandbox") // sandbox, production
    val permissions = text("permissions").nullable() // JSON array of scopes
    val lastUsedAt = timestamp("last_used_at").nullable()
    val createdAt = timestamp("created_at").nullable()
    val expiresAt = timestamp("expires_at").nullable()
}
