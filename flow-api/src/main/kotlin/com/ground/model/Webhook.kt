package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object Webhooks : UUIDTable("webhooks") {
    val accountId = uuid("account_id").references(Accounts.id)
    val url = varchar("url", 500)
    val events = text("events") // JSON array of event types
    val secret = varchar("secret", 255).nullable()
    val createdAt = timestamp("created_at").nullable()
}
