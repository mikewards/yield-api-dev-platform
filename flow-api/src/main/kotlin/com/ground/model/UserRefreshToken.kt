package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Refresh tokens for RCAC users (the User model), mirroring the legacy
 * RefreshTokens table used by the Account model.
 *
 * Security: tokens are stored only as HMAC-SHA256 hashes (TOKEN_HASH_KEY),
 * so a database leak does not leak usable credentials. Tokens are rotated
 * on every use — the previous token is revoked when a new one is issued.
 */
object UserRefreshTokens : UUIDTable("user_refresh_tokens") {
    val userId = uuid("user_id").references(Users.id)

    // HMAC-SHA256 hash of the token (indexed for O(1) lookup)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()

    // First 20 chars of the token for display/debugging
    val tokenPrefix = varchar("token_prefix", 24)

    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val revokedAt = timestamp("revoked_at").nullable()
}
