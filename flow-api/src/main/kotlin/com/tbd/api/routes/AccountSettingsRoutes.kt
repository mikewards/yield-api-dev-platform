package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.service.AccountSettingsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.accountSettingsRoutes() {
    val accountSettingsService = AccountSettingsService()
    
    routing {
        authenticate("bearer-auth") {
            route("/v1/account") {
                
                // GET /v1/account - Get account settings
                get {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized, 
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@get
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    val settings = accountSettingsService.getAccountSettings(accountId)
                    
                    if (settings == null) {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Account not found", "invalid_request_error")))
                        return@get
                    }
                    
                    call.respond(settings)
                }
                
                // PATCH /v1/account/email - Update email
                patch("/email") {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@patch
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    val request = call.receive<UpdateEmailRequest>()
                    
                    try {
                        val response = accountSettingsService.updateEmail(accountId, request)
                        call.respond(response)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "invalid_request_error")))
                    }
                }
                
                // POST /v1/account/password - Change password
                post("/password") {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@post
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    val request = call.receive<ChangePasswordRequest>()
                    
                    try {
                        val response = accountSettingsService.changePassword(accountId, request)
                        call.respond(response)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "invalid_request_error")))
                    }
                }
                
                // DELETE /v1/account - Delete account
                delete {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@delete
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    val request = call.receive<DeleteAccountRequest>()
                    
                    try {
                        val response = accountSettingsService.deleteAccount(accountId, request)
                        call.respond(response)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "invalid_request_error")))
                    }
                }
            }
            
            // Session management routes
            route("/v1/sessions") {
                
                // GET /v1/sessions - List active sessions
                get {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@get
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    val sessions = accountSettingsService.getSessions(accountId, null)
                    call.respond(sessions)
                }
                
                // DELETE /v1/sessions/{id} - Revoke specific session
                delete("/{id}") {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@delete
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    val sessionId = call.parameters["id"]
                    
                    if (sessionId == null) {
                        call.respond(HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", "Session ID required", "invalid_request_error")))
                        return@delete
                    }
                    
                    try {
                        val response = accountSettingsService.revokeSession(accountId, UUID.fromString(sessionId))
                        call.respond(response)
                    } catch (e: IllegalArgumentException) {
                        call.respond(HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", e.message ?: "Session not found", "invalid_request_error")))
                    }
                }
                
                // DELETE /v1/sessions - Revoke all other sessions
                delete {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized,
                            ErrorResponse(ErrorDetail("UNAUTHORIZED", "Authentication required", "authentication_error")))
                        return@delete
                    }
                    
                    val accountId = UUID.fromString(principal.name)
                    // TODO: Get current session ID from token to exclude it
                    val response = accountSettingsService.revokeAllOtherSessions(accountId, null)
                    call.respond(response)
                }
            }
        }
    }
}

