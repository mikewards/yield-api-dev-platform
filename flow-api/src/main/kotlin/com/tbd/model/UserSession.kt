package com.tbd.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Tracks user sessions for the "Active Sessions" feature in account settings.
 * Each refresh token is associated with a session for visibility and revocation.
 */
object UserSessions : UUIDTable("user_sessions") {
    val accountId = uuid("account_id").references(Accounts.id)
    val refreshTokenId = uuid("refresh_token_id").references(RefreshTokens.id).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()  // IPv6 can be up to 45 chars
    val userAgent = text("user_agent").nullable()
    val deviceInfo = varchar("device_info", 255).nullable()  // Parsed device name
    val lastActiveAt = timestamp("last_active_at")
    val createdAt = timestamp("created_at")
    val revokedAt = timestamp("revoked_at").nullable()
}

