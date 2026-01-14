package com.ground.api.routes

import com.ground.dto.*
import com.ground.middleware.CurrentUserIdKey
import com.ground.middleware.CurrentBusinessIdKey
import com.ground.service.WebhookService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Get the effective context ID for webhook operations.
 * 
 * Priority:
 * 1. X-Business-Id header (RCAC: webhooks scoped to business)
 * 2. Legacy accountId from principal
 * 
 * This allows team members to share webhook configuration within a business.
 */
private fun ApplicationCall.getWebhookContextId(): UUID? {
    // Try RCAC business context first
    val businessId = attributes.getOrNull(CurrentBusinessIdKey)
    if (businessId != null) return businessId
    
    // Fall back to legacy accountId
    val principal = principal<UserIdPrincipal>()
    return principal?.name?.let { 
        try { UUID.fromString(it) } catch (e: Exception) { null }
    }
}

fun Application.webhookRoutes() {
    routing {
        route("/v1/webhooks") {
            // Get webhook service status (for debugging)
            get("/status") {
                val available = WebhookService.isAvailable()
                call.respond(HttpStatusCode.OK, WebhookStatusResponse(
                    available = available,
                    message = if (available) "Webhook service is configured and ready" else "Webhook service is not configured - check SVIX_API_KEY"
                ))
            }
            
            // Debug endpoint to test list functionality - uses query param instead of path
            get("/debug") {
                val accountId = call.request.queryParameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, WebhookStatusResponse(false, "Missing id query param"))
                
                try {
                    val uuid = UUID.fromString(accountId)
                    val appId = "app_$accountId"
                    
                    // Call the raw debug method
                    val debugResult = WebhookService.debugListEndpoints(uuid)
                    
                    call.respond(HttpStatusCode.OK, WebhookStatusResponse(
                        available = true,
                        message = debugResult
                    ))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, WebhookStatusResponse(
                        available = false,
                        message = "Error: ${e.message} (${e.javaClass.simpleName})"
                    ))
                }
            }
            
            // Get available event types (public)
            get("/event-types") {
                val eventTypes = listOf(
                    WebhookEventType(
                        name = WebhookService.EventTypes.DEPOSIT_COMPLETED,
                        description = "Triggered when funds are successfully deposited to a yield account"
                    ),
                    WebhookEventType(
                        name = WebhookService.EventTypes.WITHDRAWAL_COMPLETED,
                        description = "Triggered when funds are successfully withdrawn from a yield account"
                    ),
                    WebhookEventType(
                        name = WebhookService.EventTypes.YIELD_ACCRUED,
                        description = "Triggered when yield is accrued to an account (daily)"
                    ),
                    WebhookEventType(
                        name = WebhookService.EventTypes.RATE_CHANGED,
                        description = "Triggered when yield rates change significantly (>0.5%)"
                    ),
                    WebhookEventType(
                        name = WebhookService.EventTypes.ACCOUNT_STATUS_CHANGED,
                        description = "Triggered when a yield account status changes"
                    ),
                    WebhookEventType(
                        name = WebhookService.EventTypes.APPLICATION_CREATED,
                        description = "Triggered when a new application is created"
                    ),
                    WebhookEventType(
                        name = WebhookService.EventTypes.API_KEY_CREATED,
                        description = "Triggered when a new API key is generated"
                    )
                )
                
                call.respond(HttpStatusCode.OK, WebhookEventTypesResponse(eventTypes))
            }
            
            authenticate("bearer-auth") {
                // List all webhook endpoints
                get {
                    val contextId = call.getWebhookContextId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpoints = WebhookService.listEndpoints(contextId)
                    
                    val response = endpoints.map { endpoint ->
                        WebhookEndpointResponse(
                            id = endpoint.id,
                            url = endpoint.url?.toString() ?: "",
                            description = endpoint.description,
                            filterTypes = endpoint.filterTypes?.toList(),
                            createdAt = endpoint.createdAt?.toString(),
                            updatedAt = endpoint.updatedAt?.toString(),
                            disabled = endpoint.disabled ?: false
                        )
                    }
                    
                    call.respond(HttpStatusCode.OK, WebhookEndpointsListResponse(
                        endpoints = response,
                        total = response.size
                    ))
                }
                
                // Create a new webhook endpoint
                post {
                    val contextId = call.getWebhookContextId()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val request = call.receive<CreateWebhookEndpointRequest>()
                    
                    // Validate URL
                    if (!request.url.startsWith("https://")) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf("error" to "Webhook URL must use HTTPS")
                        )
                    }
                    
                    // Validate event types if provided
                    if (request.filterTypes != null) {
                        val invalidTypes = request.filterTypes.filter { it !in WebhookService.EventTypes.ALL }
                        if (invalidTypes.isNotEmpty()) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf(
                                    "error" to "Invalid event types: ${invalidTypes.joinToString(", ")}",
                                    "valid_types" to WebhookService.EventTypes.ALL
                                )
                            )
                        }
                    }
                    
                    val result = WebhookService.createEndpoint(
                        accountId = contextId,
                        url = request.url,
                        description = request.description,
                        filterTypes = request.filterTypes
                    )
                    
                    if (result.endpoint == null) {
                        return@post call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf("error" to "Webhook service error: ${result.error ?: "Unknown error"}")
                        )
                    }
                    
                    val endpoint = result.endpoint
                    call.respond(
                        HttpStatusCode.Created,
                        WebhookEndpointResponse(
                            id = endpoint.id,
                            url = endpoint.url?.toString() ?: "",
                            description = endpoint.description,
                            filterTypes = endpoint.filterTypes?.toList(),
                            createdAt = endpoint.createdAt?.toString(),
                            updatedAt = endpoint.updatedAt?.toString(),
                            disabled = endpoint.disabled ?: false
                        )
                    )
                }
                
                // Get a specific endpoint
                get("/{endpointId}") {
                    val contextId = call.getWebhookContextId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpointId = call.parameters["endpointId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing endpoint ID"))
                    
                    val endpoint = WebhookService.getEndpoint(contextId, endpointId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Endpoint not found"))
                    
                    call.respond(
                        HttpStatusCode.OK,
                        WebhookEndpointResponse(
                            id = endpoint.id,
                            url = endpoint.url?.toString() ?: "",
                            description = endpoint.description,
                            filterTypes = endpoint.filterTypes?.toList(),
                            createdAt = endpoint.createdAt?.toString(),
                            updatedAt = endpoint.updatedAt?.toString(),
                            disabled = endpoint.disabled ?: false
                        )
                    )
                }
                
                // Delete an endpoint
                delete("/{endpointId}") {
                    val contextId = call.getWebhookContextId()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpointId = call.parameters["endpointId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing endpoint ID"))
                    
                    val deleted = WebhookService.deleteEndpoint(contextId, endpointId)
                    
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Endpoint not found or could not be deleted"))
                    }
                }
                
                // Test a webhook endpoint
                post("/{endpointId}/test") {
                    val contextId = call.getWebhookContextId()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpointId = call.parameters["endpointId"]
                        ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing endpoint ID"))
                    
                    val request = call.receive<TestWebhookRequest>()
                    
                    // Validate event type
                    if (request.eventType !in WebhookService.EventTypes.ALL) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "error" to "Invalid event type: ${request.eventType}",
                                "valid_types" to WebhookService.EventTypes.ALL
                            )
                        )
                    }
                    
                    // Send test event
                    val success = WebhookService.sendEvent(
                        accountId = contextId,
                        eventType = request.eventType,
                        payload = mapOf(
                            "test" to true,
                            "event_type" to request.eventType,
                            "message" to "This is a test webhook event from Ground",
                            "timestamp" to System.currentTimeMillis()
                        )
                    )
                    
                    if (success) {
                        call.respond(
                            HttpStatusCode.OK,
                            TestWebhookResponse(
                                success = true,
                                message = "Test webhook sent successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            TestWebhookResponse(
                                success = false,
                                message = "Failed to send test webhook"
                            )
                        )
                    }
                }
                
                // Get App Portal URL for viewing webhook logs
                get("/portal") {
                    val contextId = call.getWebhookContextId()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val result = WebhookService.getAppPortalUrl(contextId)
                    
                    if (result.url != null) {
                        call.respond(HttpStatusCode.OK, WebhookPortalResponse(
                            url = result.url,
                            recentMessages = WebhookService.getRecentMessageCount(contextId)
                        ))
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf("error" to "Webhook portal unavailable: ${result.error}")
                        )
                    }
                }
            }
        }
    }
}
