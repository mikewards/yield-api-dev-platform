package com.ground.service

import com.ground.model.AccessTokens
import com.ground.model.Accounts
import com.ground.model.RefreshTokens
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Result of generating a refresh token - includes both the token and its database ID.
 * The ID is needed to link sessions to refresh tokens.
 */
data class RefreshTokenResult(
    val token: String,
    val tokenId: UUID
)

/**
 * TokenService handles all token operations with security best practices:
 * - JWT access tokens (short-lived, 15 minutes) with embedded session ID
 * - HMAC-SHA256 hashed refresh tokens (long-lived, 30 days)
 * - Token rotation on refresh
 * - Backwards compatibility with old plaintext tokens during migration
 */
class TokenService {
    private val config = ConfigFactory.load()
    private val tokenPrefix = config.getString("ground.tokenPrefix")
    private val jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
    
    // HMAC key for hashing refresh tokens - MUST be set in production
    private val tokenHashKey: String = System.getenv("TOKEN_HASH_KEY") 
        ?: run {
            println("⚠️ WARNING: TOKEN_HASH_KEY not set! Using fallback key for development only.")
            "dev-only-token-hash-key-do-not-use-in-production-32chars!"
        }
    
    companion object {
        // Access token lifetime: 15 minutes
        const val ACCESS_TOKEN_LIFETIME_MS = 15 * 60 * 1000L
        const val ACCESS_TOKEN_LIFETIME_SEC = 15 * 60
        
        // Refresh token lifetime: 30 days
        const val REFRESH_TOKEN_LIFETIME_SEC = 30 * 24 * 60 * 60L
        
        // Token prefix length to store for display
        const val TOKEN_PREFIX_LENGTH = 20
    }
    
    /**
     * Compute HMAC-SHA256 hash of a token.
     * This is used to store refresh tokens securely - even with DB access,
     * tokens cannot be validated without the TOKEN_HASH_KEY.
     */
    private fun hmacSha256(token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(tokenHashKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val hashBytes = mac.doFinal(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    fun generateToken(accountId: UUID, name: String, expiresIn: Int? = null): Pair<UUID, String> {
        val token = "$tokenPrefix${UUID.randomUUID().toString().replace("-", "")}"
        val expiresAt = expiresIn?.let { 
            Instant.now().plusSeconds(it.toLong())
        }
        
        val now = Instant.now()
        val tokenId = transaction {
            AccessTokens.insert {
                it[AccessTokens.accountId] = accountId
                it[AccessTokens.token] = token
                it[AccessTokens.name] = name
                it[AccessTokens.createdAt] = now
                it[AccessTokens.expiresAt] = expiresAt
            } get AccessTokens.id
        }
        
        return Pair(tokenId.value, token)
    }
    
    /**
     * Generate a short-lived JWT access token (15 minutes).
     * 
     * @param accountId The user's account ID
     * @param sessionId Optional session ID to embed in the JWT for "current session" detection
     */
    fun generateAccessToken(accountId: UUID, sessionId: UUID? = null): String {
        val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        
        val builder = Jwts.builder()
            .setSubject(accountId.toString())
            .setIssuer("ground-api")
            .setAudience("ground-api")
            .setExpiration(Date(System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME_MS))
        
        // Embed session ID in JWT if provided (for "current session" detection)
        if (sessionId != null) {
            builder.claim("sid", sessionId.toString())
        }
        
        return builder.signWith(key).compact()
    }
    
    /**
     * Generate a long-lived refresh token (30 days).
     * 
     * Security: The token is hashed with HMAC-SHA256 before storage.
     * Only the hash is stored in the database - the plaintext token is
     * returned to the client and never stored.
     * 
     * @return RefreshTokenResult containing both the token string and its database ID
     */
    fun generateRefreshToken(accountId: UUID): RefreshTokenResult {
        val token = "tbd_refresh_${UUID.randomUUID().toString().replace("-", "")}"
        val tokenHashValue = hmacSha256(token)
        val tokenPrefixStr = token.take(TOKEN_PREFIX_LENGTH)
        val expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_LIFETIME_SEC)
        val now = Instant.now()
        
        val tokenId = transaction {
            RefreshTokens.insert {
                it[RefreshTokens.accountId] = accountId
                it[RefreshTokens.tokenHash] = tokenHashValue
                it[RefreshTokens.tokenPrefix] = tokenPrefixStr
                // Don't set legacy token column for new tokens
                it[RefreshTokens.expiresAt] = expiresAt
                it[RefreshTokens.createdAt] = now
            } get RefreshTokens.id
        }
        
        println("🔑 Generated refresh token for account: $accountId (prefix: $tokenPrefixStr...), expires: $expiresAt")
        return RefreshTokenResult(token = token, tokenId = tokenId.value)
    }
    
    /**
     * Refresh an access token using a valid refresh token.
     * 
     * Security:
     * - First tries hash-based lookup (new tokens)
     * - Falls back to plaintext lookup (legacy tokens during migration)
     * - Old refresh token is revoked immediately (token rotation)
     * - New refresh token is issued with hash
     * 
     * @return RefreshResult or null if invalid
     */
    fun refreshAccessToken(refreshToken: String, sessionId: UUID? = null): RefreshResult? {
        val tokenHashValue = hmacSha256(refreshToken)
        
        return transaction {
            val now = Instant.now()
            
            // First try: Find by hash (new tokens)
            var row = RefreshTokens.select { 
                (RefreshTokens.tokenHash eq tokenHashValue) and 
                (RefreshTokens.expiresAt greater now) and
                (RefreshTokens.revokedAt.isNull())
            }.firstOrNull()
            
            // Fallback: Find by plaintext token (legacy tokens)
            if (row == null) {
                row = RefreshTokens.select { 
                    (RefreshTokens.token eq refreshToken) and 
                    (RefreshTokens.expiresAt greater now) and
                    (RefreshTokens.revokedAt.isNull())
                }.firstOrNull()
                
                if (row != null) {
                    println("🔄 Found legacy plaintext token, will migrate to hash on rotation")
                }
            }
            
            if (row == null) {
                println("❌ Refresh token not found or expired: ${refreshToken.take(20)}...")
                return@transaction null
            }
            
            val accountId = row[RefreshTokens.accountId]
            val tokenId = row[RefreshTokens.id]
            
            // Revoke the old refresh token (token rotation)
            RefreshTokens.update({ RefreshTokens.id eq tokenId }) {
                it[RefreshTokens.revokedAt] = now
            }
            
            println("🔄 Rotating refresh token for account: $accountId")
            
            // Generate new tokens (always uses new hash format)
            val refreshResult = generateRefreshToken(accountId)
            val newAccessToken = generateAccessToken(accountId, sessionId)
            
            RefreshResult(
                accessToken = newAccessToken,
                refreshToken = refreshResult.token,
                accountId = accountId,
                refreshTokenId = refreshResult.tokenId
            )
        }
    }
    
    /**
     * Revoke all refresh tokens for an account (used on logout and password change)
     */
    fun revokeAllRefreshTokens(accountId: UUID) {
        transaction {
            val now = Instant.now()
            val count = RefreshTokens.update({ 
                (RefreshTokens.accountId eq accountId) and (RefreshTokens.revokedAt.isNull())
            }) {
                it[RefreshTokens.revokedAt] = now
            }
            println("🚪 Revoked $count refresh tokens for account: $accountId")
        }
    }
    
    /**
     * @deprecated Use generateAccessToken instead for new login flows
     */
    fun generateTempToken(accountId: UUID): String {
        // Legacy method - now just calls generateAccessToken
        return generateAccessToken(accountId)
    }
    
    fun validateToken(token: String): ResultRow? {
        return try {
            transaction {
                println("🔍 Looking up token in database: ${token.take(20)}...")
                val tokenRow = AccessTokens.select { AccessTokens.token eq token }.firstOrNull()
                
                if (tokenRow == null) {
                    println("❌ Token not found in database")
                    return@transaction null
                }
                
                println("✅ Token found in database - account: ${tokenRow[AccessTokens.accountId]}, environment: ${tokenRow[AccessTokens.environment]}")
                
                // Check expiration
                val expiresAt = tokenRow[AccessTokens.expiresAt]
                if (expiresAt != null && expiresAt.isBefore(java.time.Instant.now())) {
                    println("❌ Token expired: $expiresAt")
                    return@transaction null
                }
                
                println("✅ Token is valid and not expired")
                tokenRow
            }
        } catch (e: Exception) {
            println("❌ Database error during token validation: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    fun getTokensByAccount(accountId: UUID): List<com.ground.dto.TokenResponse> {
        return transaction {
            AccessTokens.select { AccessTokens.accountId eq accountId }
                .map {
                    com.ground.dto.TokenResponse(
                        token_id = it[AccessTokens.id].value.toString(),
                        access_token = it[AccessTokens.token],
                        name = it[AccessTokens.name],
                        created_at = it[AccessTokens.createdAt]?.toString() ?: "",
                        expires_at = it[AccessTokens.expiresAt]?.toString()
                    )
                }
        }
    }
    
    fun revokeToken(tokenId: UUID) {
        transaction {
            AccessTokens.deleteWhere { AccessTokens.id eq EntityID(tokenId, AccessTokens) }
        }
    }
}

/**
 * Result of refreshing tokens - includes all new token data
 */
data class RefreshResult(
    val accessToken: String,
    val refreshToken: String,
    val accountId: UUID,
    val refreshTokenId: UUID
)
