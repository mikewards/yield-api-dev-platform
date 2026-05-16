package com.ground.api.routes

import com.ground.dto.*
import com.ground.middleware.CurrentUserIdKey
import com.ground.service.ApplicationService
import com.ground.service.SandboxService
import com.ground.service.WebhookService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

fun Application.applicationRoutes() {
    val applicationService = ApplicationService()
    
    routing {
        route("/v1/applications") {
            authenticate("bearer-auth") {
                // Create application
                post {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val request = call.receive<CreateApplicationRequest>()
                    
                    try {
                        // Try RCAC first (JWT users have CurrentUserIdKey)
                        val userId = call.attributes.getOrNull(CurrentUserIdKey)
                        
                        val app = if (userId != null) {
                            // RCAC: User creating via dashboard/JWT
                            applicationService.createApplicationRcac(userId, request)
                        } else {
                            // Legacy: API key with accountId
                            val accountId = UUID.fromString(principal.name)
                            applicationService.createApplication(accountId, request)
                        }
                        
                        // Fire webhook event asynchronously (legacy, uses accountId if available)
                        val accountIdForWebhook = try { UUID.fromString(principal.name) } catch (e: Exception) { null }
                        if (accountIdForWebhook != null) {
                            coroutineScope {
                                launch {
                                    WebhookService.sendEvent(
                                        accountId = accountIdForWebhook,
                                        eventType = WebhookService.EventTypes.APPLICATION_CREATED,
                                        payload = mapOf(
                                            "application_id" to app.application_id,
                                            "application_name" to app.name,
                                            "environment" to app.environment,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                        }
                        
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
                    
                    // Try RCAC first
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    
                    val apps = if (userId != null) {
                        applicationService.listApplicationsRcac(userId)
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.listApplications(accountId)
                    }
                    
                    call.respond(ApplicationListResponse(apps))
                }
                
                // Get application
                get("/{applicationId}") {
                    try {
                        val principal = call.principal<UserIdPrincipal>()!!
                        val applicationId = UUID.fromString(call.parameters["applicationId"])
                        
                        // Try RCAC first
                        val userId = call.attributes.getOrNull(CurrentUserIdKey)
                        
                        val app = if (userId != null) {
                            applicationService.getApplicationRcac(userId, applicationId, "read")
                        } else {
                            val accountId = UUID.fromString(principal.name)
                            applicationService.getApplication(accountId, applicationId)
                        }
                        
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
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val request = call.receive<UpdateApplicationRequest>()
                    
                    // Try RCAC first
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    
                    val app = if (userId != null) {
                        applicationService.updateApplicationRcac(userId, applicationId, request)
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.updateApplication(accountId, applicationId, request)
                    }
                    
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
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    
                    // Try RCAC first
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    
                    val deleted = if (userId != null) {
                        applicationService.deleteApplicationRcac(userId, applicationId)
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.deleteApplication(accountId, applicationId)
                    }
                    
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
                        
                        try {
                            val request = call.receive<CreateAppTokenRequest>()
                            val token = applicationService.createAppToken(accountId, applicationId, request)
                            
                            // Fire webhook event asynchronously - fetch app name for context
                            val appName = applicationService.getApplication(accountId, applicationId)?.name
                            coroutineScope {
                                launch {
                                    WebhookService.sendEvent(
                                        accountId = accountId,
                                        eventType = WebhookService.EventTypes.API_KEY_CREATED,
                                        payload = mapOf(
                                            "application_id" to applicationId.toString(),
                                            "application_name" to appName,
                                            "token_id" to token.token_id,
                                            "environment" to token.environment,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                    )
                                }
                            }
                            
                            call.respond(HttpStatusCode.Created, token)
                        } catch (e: IllegalArgumentException) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(ErrorDetail("NOT_FOUND", e.message ?: "Application not found", "not_found_error"))
                            )
                        } catch (e: Exception) {
                            println("Error creating app token: ${e.message}")
                            e.printStackTrace()
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse(ErrorDetail("INTERNAL_ERROR", e.message ?: "Failed to create token", "internal_error"))
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
                
                // Sandbox initialization endpoint
                post("/sandbox/initialize") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    
                    val sandboxService = SandboxService()
                    sandboxService.initializeSandbox(accountId)
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "message" to "Sandbox initialized with test data",
                        "account_id" to accountId.toString()
                    ))
                }
            }
        }
    }
}

