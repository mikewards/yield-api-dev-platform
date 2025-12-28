package com.tbd.service

import com.tbd.model.AccessTokens
import com.tbd.model.Accounts
import com.tbd.model.RefreshTokens
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

class TokenService {
    private val config = ConfigFactory.load()
    private val tokenPrefix = config.getString("tbd.tokenPrefix")
    private val jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
    
    companion object {
        // Access token lifetime: 15 minutes
        const val ACCESS_TOKEN_LIFETIME_MS = 15 * 60 * 1000L
        const val ACCESS_TOKEN_LIFETIME_SEC = 15 * 60
        
        // Refresh token lifetime: 30 days
        const val REFRESH_TOKEN_LIFETIME_SEC = 30 * 24 * 60 * 60L
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
     * Generate a short-lived JWT access token (15 minutes)
     */
    fun generateAccessToken(accountId: UUID): String {
        val key: SecretKey = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
        
        return Jwts.builder()
            .setSubject(accountId.toString())
            .setIssuer("tbd-api")
            .setAudience("tbd-api")
            .setExpiration(Date(System.currentTimeMillis() + ACCESS_TOKEN_LIFETIME_MS))
            .signWith(key)
            .compact()
    }
    
    /**
     * Generate a long-lived refresh token (30 days), stored in database
     */
    fun generateRefreshToken(accountId: UUID): String {
        val token = "tbd_refresh_${UUID.randomUUID().toString().replace("-", "")}"
        val expiresAt = Instant.now().plusSeconds(REFRESH_TOKEN_LIFETIME_SEC)
        val now = Instant.now()
        
        transaction {
            RefreshTokens.insert {
                it[RefreshTokens.accountId] = accountId
                it[RefreshTokens.token] = token
                it[RefreshTokens.expiresAt] = expiresAt
                it[RefreshTokens.createdAt] = now
            }
        }
        
        println("🔑 Generated refresh token for account: $accountId, expires: $expiresAt")
        return token
    }
    
    /**
     * Refresh an access token using a valid refresh token
     * Implements token rotation for security
     */
    fun refreshAccessToken(refreshToken: String): Triple<String, String, UUID>? {
        return transaction {
            val now = Instant.now()
            
            // Find valid refresh token
            val row = RefreshTokens.select { 
                (RefreshTokens.token eq refreshToken) and 
                (RefreshTokens.expiresAt greater now) and
                (RefreshTokens.revokedAt.isNull())
            }.firstOrNull()
            
            if (row == null) {
                println("❌ Refresh token not found or expired: ${refreshToken.take(20)}...")
                return@transaction null
            }
            
            val accountId = row[RefreshTokens.accountId]
            val tokenId = row[RefreshTokens.id]
            
            // Revoke the old refresh token (token rotation)
            RefreshTokens.update({ RefreshTokens.id eq tokenId }) {
                it[revokedAt] = now
            }
            
            println("🔄 Rotating refresh token for account: $accountId")
            
            // Generate new tokens
            val newAccessToken = generateAccessToken(accountId)
            val newRefreshToken = generateRefreshToken(accountId)
            
            Triple(newAccessToken, newRefreshToken, accountId)
        }
    }
    
    /**
     * Revoke all refresh tokens for an account (used on logout)
     */
    fun revokeAllRefreshTokens(accountId: UUID) {
        transaction {
            val now = Instant.now()
            val count = RefreshTokens.update({ 
                (RefreshTokens.accountId eq accountId) and (RefreshTokens.revokedAt.isNull())
            }) {
                it[revokedAt] = now
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
    
    fun getTokensByAccount(accountId: UUID): List<com.tbd.dto.TokenResponse> {
        return transaction {
            AccessTokens.select { AccessTokens.accountId eq accountId }
                .map {
                    com.tbd.dto.TokenResponse(
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
