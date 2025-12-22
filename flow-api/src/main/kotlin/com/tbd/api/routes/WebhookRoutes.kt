package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.service.WebhookService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.webhookRoutes() {
    routing {
        route("/v1/webhooks") {
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
                    val principal = call.principal<JWTPrincipal>()
                    val accountId = principal?.payload?.getClaim("account_id")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpoints = WebhookService.listEndpoints(UUID.fromString(accountId))
                    
                    val response = endpoints.map { endpoint ->
                        WebhookEndpointResponse(
                            id = endpoint.id,
                            url = endpoint.url,
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
                    val principal = call.principal<JWTPrincipal>()
                    val accountId = principal?.payload?.getClaim("account_id")?.asString()
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
                    
                    val endpoint = WebhookService.createEndpoint(
                        accountId = UUID.fromString(accountId),
                        url = request.url,
                        description = request.description,
                        filterTypes = request.filterTypes
                    )
                    
                    if (endpoint == null) {
                        return@post call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            mapOf("error" to "Webhook service unavailable. Please try again later.")
                        )
                    }
                    
                    call.respond(
                        HttpStatusCode.Created,
                        WebhookEndpointResponse(
                            id = endpoint.id,
                            url = endpoint.url,
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
                    val principal = call.principal<JWTPrincipal>()
                    val accountId = principal?.payload?.getClaim("account_id")?.asString()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpointId = call.parameters["endpointId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing endpoint ID"))
                    
                    val endpoint = WebhookService.getEndpoint(UUID.fromString(accountId), endpointId)
                        ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Endpoint not found"))
                    
                    call.respond(
                        HttpStatusCode.OK,
                        WebhookEndpointResponse(
                            id = endpoint.id,
                            url = endpoint.url,
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
                    val principal = call.principal<JWTPrincipal>()
                    val accountId = principal?.payload?.getClaim("account_id")?.asString()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    
                    val endpointId = call.parameters["endpointId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing endpoint ID"))
                    
                    val deleted = WebhookService.deleteEndpoint(UUID.fromString(accountId), endpointId)
                    
                    if (deleted) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound, mapOf("error" to "Endpoint not found or could not be deleted"))
                    }
                }
                
                // Test a webhook endpoint
                post("/{endpointId}/test") {
                    val principal = call.principal<JWTPrincipal>()
                    val accountId = principal?.payload?.getClaim("account_id")?.asString()
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
                        accountId = UUID.fromString(accountId),
                        eventType = request.eventType,
                        payload = mapOf(
                            "test" to true,
                            "event_type" to request.eventType,
                            "message" to "This is a test webhook event from TBD",
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
            }
        }
    }
}
