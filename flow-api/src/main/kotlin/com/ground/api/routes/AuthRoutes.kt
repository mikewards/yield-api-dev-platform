package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.AccountLockedException
import com.ground.service.AccountService
import com.ground.service.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.authRoutes() {
    val accountService = AccountService()
    val tokenService = TokenService()
    
    routing {
        route("/v1/auth") {
            // Login - returns access token + refresh token, creates session
            post("/authenticate") {
                val request = call.receive<AuthenticateRequest>()
                
                // Get client info for session tracking
                val ipAddress = getClientIp(call)
                val userAgent = call.request.headers["User-Agent"]
                
                try {
                    val response = accountService.authenticate(request, ipAddress, userAgent)
                    call.respond(response)
                } catch (e: AccountLockedException) {
                    // Account is locked due to too many failed attempts
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ErrorResponse(ErrorDetail("ACCOUNT_LOCKED", e.message ?: "Account is locked", "authentication_error"))
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(ErrorDetail("INVALID_CREDENTIALS", e.message ?: "Invalid credentials", "authentication_error"))
                    )
                }
            }
            
            // Refresh - exchange refresh token for new access token + refresh token
            post("/refresh") {
                try {
                    val request = call.receive<RefreshTokenRequest>()
                    
                    // Note: We don't pass sessionId here - the session stays linked to the original
                    // refresh token chain. When you refresh, you're continuing the same session.
                    val result = tokenService.refreshAccessToken(request.refresh_token)
                    if (result == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("INVALID_TOKEN", "Refresh token expired or invalid. Please sign in again.", "authentication_error"))
                        )
                        return@post
                    }
                    
                    call.respond(RefreshTokenResponse(
                        access_token = result.accessToken,
                        refresh_token = result.refreshToken,
                        token_type = "Bearer",
                        expires_in = TokenService.ACCESS_TOKEN_LIFETIME_SEC
                    ))
                } catch (e: Exception) {
                    println("❌ Refresh token error: ${e.message}")
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(ErrorDetail("INVALID_REQUEST", "Invalid refresh token request", "validation_error"))
                    )
                }
            }
            
            // Logout - revoke all refresh tokens (requires valid access token)
            authenticate("bearer-auth") {
                post("/logout") {
                    try {
                        val principal = call.principal<UserIdPrincipal>()
                        if (principal == null) {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                ErrorResponse(ErrorDetail("UNAUTHORIZED", "Invalid or expired token", "authentication_error"))
                            )
                            return@post
                        }
                        
                        val accountId = UUID.fromString(principal.name)
                        tokenService.revokeAllRefreshTokens(accountId)
                        
                        call.respond(mapOf("message" to "Successfully logged out"))
                    } catch (e: Exception) {
                        println("❌ Logout error: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(ErrorDetail("INTERNAL_ERROR", "Failed to logout", "internal_error"))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get the real client IP address, handling proxies
 */
private fun getClientIp(call: ApplicationCall): String {
    // Check X-Forwarded-For (when behind a proxy/load balancer)
    val forwarded = call.request.header("X-Forwarded-For")
    if (forwarded != null) {
        // Take the first IP (original client)
        return forwarded.split(",").firstOrNull()?.trim() ?: "unknown"
    }
    
    // Check X-Real-IP
    val realIp = call.request.header("X-Real-IP")
    if (realIp != null) {
        return realIp.trim()
    }
    
    // Fall back to direct connection IP
    return call.request.origin.remoteHost ?: "unknown"
}
