package com.ground.middleware

import com.ground.model.AccessTokens
import com.ground.model.Applications
import com.ground.service.TokenService
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import com.ground.middleware.setSentryUserContext
import com.ground.middleware.setSentryApplicationContext
import java.util.*

// Attribute keys for storing application context
val ApplicationIdKey = AttributeKey<String>("applicationId")
val ApplicationNameKey = AttributeKey<String>("applicationName")
val EnvironmentKey = AttributeKey<String>("environment")

// RCAC attribute keys
val CurrentUserIdKey = AttributeKey<UUID>("currentUserId")
val CurrentBusinessIdKey = AttributeKey<UUID>("currentBusinessId")

fun Application.auth() {
    val config = ConfigFactory.load()
    val jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
    
    install(Authentication) {
        bearer("bearer-auth") {
            realm = "Ground API"
            authenticate { tokenCredential ->
                try {
                    val tokenService = TokenService()
                    val tokenPrefix = tokenCredential.token.take(8)
                    
                    // First try to validate as a PAT (Personal Access Token) from database
                    val patToken = tokenService.validateToken(tokenCredential.token)
                    if (patToken != null) {
                        val accountId = patToken[AccessTokens.accountId]
                        val applicationId = patToken[AccessTokens.applicationId]
                        val environment = patToken[AccessTokens.environment]
                        
                        println("✅ PAT token validated for account: $accountId, app: $applicationId")
                        
                        // Store application context in call attributes
                        if (applicationId != null) {
                            this.request.call.attributes.put(ApplicationIdKey, applicationId.toString())
                            this.request.call.attributes.put(EnvironmentKey, environment)
                            
                            // Fetch application name
                            val appName = transaction {
                                Applications.select { Applications.id eq applicationId }
                                    .firstOrNull()?.get(Applications.name)
                            }
                            if (appName != null) {
                                this.request.call.attributes.put(ApplicationNameKey, appName)
                            }
                            
                            // Set Sentry context for API key auth (IDs only)
                            setSentryApplicationContext(
                                accountId = accountId.toString(),
                                applicationId = applicationId.toString(),
                                environment = environment
                            )
                        }
                        
                        return@authenticate UserIdPrincipal(accountId.toString())
                    }
                    
                    // Then try to validate as a JWT (from login)
                    try {
                        val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
                        val claims = Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(tokenCredential.token)
                        val subjectId = claims.payload.subject
                        if (subjectId != null) {
                            println("✅ JWT token validated for subject: $subjectId")
                            // Set Sentry context for dashboard user (IDs only)
                            setSentryUserContext(subjectId)
                            
                            // Only set CurrentUserIdKey if this is a RCAC user (exists in Users table)
                            // Legacy tokens have accountId in subject, not userId
                            try {
                                val uuid = UUID.fromString(subjectId)
                                val isRcacUser = transaction {
                                    com.ground.model.Users.select { com.ground.model.Users.id eq uuid }.count() > 0
                                }
                                if (isRcacUser) {
                                    println("  → RCAC user detected, setting CurrentUserIdKey")
                                    this.request.call.attributes.put(CurrentUserIdKey, uuid)
                                    
                                    // Check for business context header
                                    val businessIdHeader = this.request.call.request.headers["X-Business-Id"]
                                    if (businessIdHeader != null) {
                                        try {
                                            this.request.call.attributes.put(CurrentBusinessIdKey, UUID.fromString(businessIdHeader))
                                        } catch (e: Exception) {
                                            // Invalid business ID format, ignore
                                        }
                                    }
                                } else {
                                    println("  → Legacy account detected, using accountId fallback")
                                }
                            } catch (e: Exception) {
                                println("  → Error checking user type: ${e.message}")
                            }
                            
                            // JWT tokens are from dashboard login, no application context
                            return@authenticate UserIdPrincipal(subjectId)
                        }
                    } catch (e: Exception) {
                        println("⚠️ JWT validation failed: ${e.message}")
                    }
                    
                    println("❌ Token validation failed - token prefix: $tokenPrefix")
                    null
                } catch (e: Exception) {
                    println("❌ Authentication error: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        }
    }
}
