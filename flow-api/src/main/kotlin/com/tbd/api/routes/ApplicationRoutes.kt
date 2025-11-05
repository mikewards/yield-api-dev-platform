package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.service.ApplicationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.applicationRoutes() {
    val applicationService = ApplicationService()
    
    routing {
        route("/v1/applications") {
            authenticate("bearer-auth") {
                // Create application
                post {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val request = call.receive<CreateApplicationRequest>()
                    
                    try {
                        val app = applicationService.createApplication(accountId, request)
                        call.respond(HttpStatusCode.Created, app)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "invalid_request_error"))
                        )
                    }
                }
                
                // List applications
                get {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val apps = applicationService.listApplications(accountId)
                    call.respond(ApplicationListResponse(apps))
                }
                
                // Get application
                get("/{applicationId}") {
                    try {
                        val principal = call.principal<UserIdPrincipal>()!!
                        val accountId = UUID.fromString(principal.name)
                        val applicationId = UUID.fromString(call.parameters["applicationId"])
                        
                        val app = applicationService.getApplication(accountId, applicationId)
                        if (app != null) {
                            call.respond(app)
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                            )
                        }
                    } catch (e: Exception) {
                        call.application.environment.log.error("Error getting application", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(ErrorDetail("INTERNAL_ERROR", e.message ?: "Internal server error", "server_error"))
                        )
                    }
                }
                
                // Update application
                patch("/{applicationId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val request = call.receive<UpdateApplicationRequest>()
                    
                    val app = applicationService.updateApplication(accountId, applicationId, request)
                    if (app != null) {
                        call.respond(app)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                }
                
                // Delete application
                delete("/{applicationId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    
                    val deleted = applicationService.deleteApplication(accountId, applicationId)
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                }
                
                // Regenerate webhook secret
                post("/{applicationId}/webhook-secret/regenerate") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    
                    val newSecret = applicationService.regenerateWebhookSecret(accountId, applicationId)
                    if (newSecret != null) {
                        call.respond(mapOf("webhook_secret" to newSecret))
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                }
                
                // Application tokens
                route("/{applicationId}/tokens") {
                    // Create token for application
                    post {
                        val principal = call.principal<UserIdPrincipal>()!!
                        val accountId = UUID.fromString(principal.name)
                        val applicationId = UUID.fromString(call.parameters["applicationId"])
                        val request = call.receive<CreateAppTokenRequest>()
                        
                        try {
                            val token = applicationService.createAppToken(accountId, applicationId, request)
                            call.respond(HttpStatusCode.Created, token)
                        } catch (e: IllegalArgumentException) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(ErrorDetail("NOT_FOUND", e.message ?: "Application not found", "not_found_error"))
                            )
                        }
                    }
                    
                    // List tokens for application
                    get {
                        val principal = call.principal<UserIdPrincipal>()!!
                        val accountId = UUID.fromString(principal.name)
                        val applicationId = UUID.fromString(call.parameters["applicationId"])
                        
                        val tokens = applicationService.listAppTokens(accountId, applicationId)
                        call.respond(AppTokenListResponse(tokens))
                    }
                    
                    // Revoke token
                    delete("/{tokenId}") {
                        val principal = call.principal<UserIdPrincipal>()!!
                        val accountId = UUID.fromString(principal.name)
                        val applicationId = UUID.fromString(call.parameters["applicationId"])
                        val tokenId = UUID.fromString(call.parameters["tokenId"])
                        
                        val revoked = applicationService.revokeAppToken(accountId, applicationId, tokenId)
                        if (revoked) {
                            call.respond(HttpStatusCode.NoContent)
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(ErrorDetail("NOT_FOUND", "Token not found", "not_found_error"))
                            )
                        }
                    }
                }
            }
        }
    }
}

