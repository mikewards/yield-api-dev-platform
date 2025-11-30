package com.tbd.api.routes

import com.tbd.service.RequestLogService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

private val logService = RequestLogService()

fun Application.logRoutes() {
    routing {
        authenticate("bearer-auth") {
            // Get logs for authenticated user's applications
            get("/v1/logs") {
                val principal = call.principal<UserIdPrincipal>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "status" to "error",
                        "code" to "UNAUTHORIZED",
                        "message" to "Authentication required"
                    ))
                    return@get
                }
                
                val accountId = UUID.fromString(principal.name)
                
                // Parse query parameters
                val applicationId = call.request.queryParameters["application_id"]?.let { 
                    try { UUID.fromString(it) } catch (e: Exception) { null }
                }
                val environment = call.request.queryParameters["environment"]
                val method = call.request.queryParameters["method"]
                val status = call.request.queryParameters["status"]
                val path = call.request.queryParameters["path"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                
                try {
                    val response = logService.getLogsForAccount(
                        accountId = accountId,
                        applicationId = applicationId,
                        environment = environment,
                        method = method,
                        statusFilter = status,
                        pathFilter = path,
                        page = page,
                        pageSize = pageSize
                    )
                    
                    call.respond(HttpStatusCode.OK, response)
                } catch (e: Exception) {
                    println("❌ Error fetching logs: ${e.message}")
                    e.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "status" to "error",
                        "code" to "INTERNAL_ERROR",
                        "message" to "Failed to retrieve logs: ${e.message}"
                    ))
                }
            }
            
            // Get log stats summary
            get("/v1/logs/stats") {
                val principal = call.principal<UserIdPrincipal>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf(
                        "status" to "error",
                        "code" to "UNAUTHORIZED",
                        "message" to "Authentication required"
                    ))
                    return@get
                }
                
                val accountId = UUID.fromString(principal.name)
                
                try {
                    val response = logService.getLogsForAccount(
                        accountId = accountId,
                        page = 1,
                        pageSize = 1
                    )
                    
                    call.respond(HttpStatusCode.OK, response.stats)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf(
                        "status" to "error",
                        "code" to "INTERNAL_ERROR",
                        "message" to "Failed to retrieve stats: ${e.message}"
                    ))
                }
            }
        }
    }
}

