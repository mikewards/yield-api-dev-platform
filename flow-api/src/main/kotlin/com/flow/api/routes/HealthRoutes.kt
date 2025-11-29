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
                    service = "Flow API Gateway",
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

