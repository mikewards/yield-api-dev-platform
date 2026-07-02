package com.ground.service

import com.ground.model.UserRefreshTokens
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Result of refreshing an RCAC user session.
 */
data class UserRefreshResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val userId: UUID
)

/**
 * Token service for RCAC users (the User model), following the same security
 * pattern as the legacy TokenService:
 * - Short-lived JWT access tokens (15 minutes)
 * - Long-lived refresh tokens (30 days) stored only as HMAC-SHA256 hashes
 * - Rotation on every refresh (each refresh token is single-use)
 */
class UserTokenService {
    private val config = ConfigFactory.load()
    private val jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
    private val jwtKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    // HMAC key for hashing refresh tokens - MUST be set in production
    private val tokenHashKey: String = System.getenv("TOKEN_HASH_KEY")
        ?: run {
            println("⚠️ WARNING: TOKEN_HASH_KEY not set! Using fallback key for development only.")
            "dev-only-token-hash-key-do-not-use-in-production-32chars!"
        }

    companion object {
        // Aligned with the legacy flow: 15-minute access tokens
        const val ACCESS_TOKEN_LIFETIME_SEC = 15 * 60

        // Refresh token lifetime: 30 days
        const val REFRESH_TOKEN_LIFETIME_SEC = 30 * 24 * 60 * 60L

        const val TOKEN_PREFIX_LENGTH = 20
    }

    private fun hmacSha256(token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(tokenHashKey.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val hashBytes = mac.doFinal(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate a short-lived JWT access token (15 minutes) for an RCAC user.
     * Returns the token and its lifetime in seconds.
     */
    fun generateAccessToken(userId: UUID): Pair<String, Int> {
        val now = Instant.now()
        val token = Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ACCESS_TOKEN_LIFETIME_SEC.toLong())))
            .signWith(jwtKey)
            .compact()
        return token to ACCESS_TOKEN_LIFETIME_SEC
    }

    /**
     * Generate a long-lived refresh token (30 days) for an RCAC user.
     * Only the HMAC-SHA256 hash is persisted; the plaintext token is
     * returned to the client and never stored.
     */
    fun generateRefreshToken(userId: UUID): String {
        val token = "grt_${UUID.randomUUID().toString().replace("-", "")}"
        val now = Instant.now()

        transaction {
            UserRefreshTokens.insert {
                it[UserRefreshTokens.userId] = userId
                it[tokenHash] = hmacSha256(token)
                it[tokenPrefix] = token.take(TOKEN_PREFIX_LENGTH)
                it[expiresAt] = now.plusSeconds(REFRESH_TOKEN_LIFETIME_SEC)
                it[createdAt] = now
            }
        }

        return token
    }

    /**
     * Exchange a valid refresh token for a new access token + refresh token.
     * The presented refresh token is revoked (rotation) — it is single-use.
     *
     * @return UserRefreshResult or null if the token is invalid/expired/revoked
     */
    fun refresh(refreshToken: String): UserRefreshResult? {
        val tokenHashValue = hmacSha256(refreshToken)

        return transaction {
            val now = Instant.now()

            val row = UserRefreshTokens.select {
                (UserRefreshTokens.tokenHash eq tokenHashValue) and
                (UserRefreshTokens.expiresAt greater now) and
                (UserRefreshTokens.revokedAt.isNull())
            }.firstOrNull() ?: return@transaction null

            val userId = row[UserRefreshTokens.userId]

            // Revoke the presented token (rotation)
            UserRefreshTokens.update({ UserRefreshTokens.id eq row[UserRefreshTokens.id] }) {
                it[revokedAt] = now
            }

            val newRefreshToken = generateRefreshToken(userId)
            val (accessToken, expiresIn) = generateAccessToken(userId)

            UserRefreshResult(
                accessToken = accessToken,
                refreshToken = newRefreshToken,
                expiresIn = expiresIn,
                userId = userId
            )
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout, password change).
     */
    fun revokeAllRefreshTokens(userId: UUID) {
        transaction {
            val now = Instant.now()
            val count = UserRefreshTokens.update({
                (UserRefreshTokens.userId eq userId) and (UserRefreshTokens.revokedAt.isNull())
            }) {
                it[revokedAt] = now
            }
            println("🚪 Revoked $count refresh tokens for user: $userId")
        }
    }
}
