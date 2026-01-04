package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object RequestLogs : UUIDTable("request_logs") {
    val requestId = varchar("request_id", 50).uniqueIndex()
    val applicationId = uuid("application_id").references(Applications.id).nullable()
    val accountId = uuid("account_id").references(Accounts.id).nullable()
    val environment = varchar("environment", 20) // sandbox, production
    val method = varchar("method", 10) // GET, POST, PUT, DELETE
    val path = varchar("path", 500)
    val statusCode = integer("status_code")
    val durationMs = integer("duration_ms")
    val ipAddress = varchar("ip_address", 50).nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val requestBody = text("request_body").nullable()
    val responseBody = text("response_body").nullable()
    val errorMessage = text("error_message").nullable()
    val timestamp = timestamp("timestamp")
    
    init {
        // Index for efficient queries by account and time
        index(false, accountId, timestamp)
        index(false, applicationId, timestamp)
    }
}

