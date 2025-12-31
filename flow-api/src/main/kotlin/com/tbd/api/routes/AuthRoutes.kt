package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.service.AccountLockedException
import com.tbd.service.AccountService
import com.tbd.service.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.authRoutes() {
    val accountService = AccountService()
    val tokenService = TokenService()
    
    routing {
        route("/v1/auth") {
            // Login - returns access token + refresh token
            post("/authenticate") {
                val request = call.receive<AuthenticateRequest>()
                try {
                    val response = accountService.authenticate(request)
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
                    
                    val result = tokenService.refreshAccessToken(request.refresh_token)
                    if (result == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("INVALID_TOKEN", "Refresh token expired or invalid. Please sign in again.", "authentication_error"))
                        )
                        return@post
                    }
                    
                    val (accessToken, refreshToken, _) = result
                    
                    call.respond(RefreshTokenResponse(
                        access_token = accessToken,
                        refresh_token = refreshToken,
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
