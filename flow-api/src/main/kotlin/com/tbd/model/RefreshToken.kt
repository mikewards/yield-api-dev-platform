package com.tbd.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Refresh tokens table with HMAC-SHA256 hashing for security.
 * 
 * Security: Tokens are hashed with HMAC-SHA256 using TOKEN_HASH_KEY.
 * Even with database access, tokens cannot be validated without the key.
 * 
 * Only the tokenPrefix is stored in plaintext for display purposes.
 */
object RefreshTokens : UUIDTable("refresh_tokens") {
    val accountId = uuid("account_id").references(Accounts.id)
    
    // HMAC-SHA256 hash of the token (indexed for O(1) lookup)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    
    // First 20 chars of token for display in UI (e.g., "tbd_refresh_a3b4c5...")
    val tokenPrefix = varchar("token_prefix", 24)
    
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val revokedAt = timestamp("revoked_at").nullable()
}
