package com.flow.middleware

import com.flow.model.AccessTokens
import com.flow.service.TokenService
import com.typesafe.config.ConfigFactory
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.auth() {
    val config = ConfigFactory.load()
    val jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret")
    
    install(Authentication) {
        bearer("bearer-auth") {
            realm = "Flow API"
            authenticate { tokenCredential ->
                val tokenService = TokenService()
                
                // First try to validate as a PAT (Personal Access Token) from database
                val patToken = tokenService.validateToken(tokenCredential.token)
                if (patToken != null) {
                    return@authenticate UserIdPrincipal(patToken[AccessTokens.accountId].toString())
                }
                
                // Then try to validate as a JWT (from login)
                try {
                    val key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())
                    val claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(tokenCredential.token)
                    val accountId = claims.payload.subject
                    if (accountId != null) {
                        return@authenticate UserIdPrincipal(accountId)
                    }
                } catch (e: Exception) {
                    // JWT validation failed
                }
                
                null
            }
        }
    }
}
