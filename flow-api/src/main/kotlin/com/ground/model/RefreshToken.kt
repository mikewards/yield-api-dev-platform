package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Refresh tokens table with HMAC-SHA256 hashing for security.
 * 
 * Security: Tokens are hashed with HMAC-SHA256 using TOKEN_HASH_KEY.
 * Even with database access, tokens cannot be validated without the key.
 * 
 * Migration note: tokenHash and tokenPrefix are nullable for backwards
 * compatibility with existing token column. New tokens use hash-based storage.
 */
object RefreshTokens : UUIDTable("refresh_tokens") {
    val accountId = uuid("account_id").references(Accounts.id)
    
    // Legacy: plaintext token (for existing tokens before migration)
    // TODO: Remove after all old tokens expire (30 days)
    val token = varchar("token", 255).uniqueIndex().nullable()
    
    // New: HMAC-SHA256 hash of the token (indexed for O(1) lookup)
    val tokenHash = varchar("token_hash", 64).nullable().uniqueIndex()
    
    // New: First 20 chars of token for display in UI
    val tokenPrefix = varchar("token_prefix", 24).nullable()
    
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")
    val revokedAt = timestamp("revoked_at").nullable()
}
