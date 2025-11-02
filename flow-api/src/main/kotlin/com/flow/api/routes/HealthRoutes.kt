package com.flow.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val version: String = "1.0.0"
)

fun Application.healthRoutes() {
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "Flow API Gateway",
                    "status" to "running",
                    "version" to "1.0.0",
                    "endpoints" to mapOf(
                        "health" to "/health",
                        "accounts" to "/v1/accounts",
                        "auth" to "/v1/auth",
                        "applications" to "/v1/applications",
                        "yield" to "/v1/yield"
                    )
                )
            )
        }
        
        get("/health") {
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    status = "healthy",
                    timestamp = System.currentTimeMillis().toString()
                )
            )
        }
    }
}

