package com.tbd.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import com.tbd.model.Accounts

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val version: String = "1.0.0"
)

@Serializable
data class ApiInfoResponse(
    val service: String,
    val status: String,
    val version: String,
    val endpoints: EndpointsInfo
)

@Serializable
data class EndpointsInfo(
    val health: String,
    val accounts: String,
    val auth: String,
    val applications: String,
    val yield: String
)

fun Application.healthRoutes() {
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                ApiInfoResponse(
                    service = "TBD API Gateway",
                    status = "running",
                    version = "1.0.0",
                    endpoints = EndpointsInfo(
                        health = "/health",
                        accounts = "/v1/accounts",
                        auth = "/v1/auth",
                        applications = "/v1/applications",
                        yield = "/v1/yield"
                    )
                )
            )
        }
        
        get("/health") {
            // Test database connection by querying accounts table
            val dbStatus = try {
                transaction {
                    Accounts.select { Accounts.id.isNotNull() }.limit(1).firstOrNull()
                }
                "connected"
            } catch (e: Exception) {
                "disconnected: ${e.message?.take(50)}"
            }
            
            call.respond(
                HttpStatusCode.OK,
                HealthResponse(
                    status = if (dbStatus == "connected") "healthy" else "degraded",
                    timestamp = System.currentTimeMillis().toString()
                )
            )
        }
        
        // Test endpoint for Sentry (remove in production)
        get("/test-sentry") {
            try {
                throw Exception("This is a test error to verify Sentry integration")
            } catch (e: Exception) {
                io.sentry.Sentry.captureException(e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf(
                        "message" to "Test error sent to Sentry",
                        "error" to e.message
                    )
                )
            }
        }
    }
}

