package com.tbd.service

import com.tbd.model.AccessTokens
import com.tbd.model.Accounts
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import javax.crypto.SecretKey

class TokenService {
    private val config = ConfigFactory.load()
    private val tokenPrefix = config.getString("tbd.tokenPrefix")
    
    fun generateToken(accountId: UUID, name: String, expiresIn: Int? = null): Pair<UUID, String> {
        val token = "$tokenPrefix${UUID.randomUUID().toString().replace("-", "")}"
        val expiresAt = expiresIn?.let { 
            java.time.Instant.now().plusSeconds(it.toLong())
        }
        
        val now = java.time.Instant.now()
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
    
    fun generateTempToken(accountId: UUID): String {
        // Generate JWT token for temporary authentication
        val secret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
        val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())
        
        return Jwts.builder()
            .setSubject(accountId.toString())
            .setIssuer("tbd-api")
            .setAudience("tbd-api")
            .setExpiration(Date(System.currentTimeMillis() + 3600 * 1000))
            .signWith(key)
            .compact()
    }
    
    fun validateToken(token: String): ResultRow? {
        return transaction {
            val tokenRow = AccessTokens.select { AccessTokens.token eq token }.firstOrNull()
            if (tokenRow == null) return@transaction null
            
            // Check expiration
            val expiresAt = tokenRow[AccessTokens.expiresAt]
            if (expiresAt != null && expiresAt.isBefore(java.time.Instant.now())) {
                return@transaction null
            }
            
            tokenRow
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
