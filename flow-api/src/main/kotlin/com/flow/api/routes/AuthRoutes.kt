package com.flow.api.routes

import com.flow.dto.*
import com.flow.service.AccountService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.authRoutes() {
    val accountService = AccountService()
    
    routing {
        route("/v1/auth") {
            post("/authenticate") {
                val request = call.receive<AuthenticateRequest>()
                try {
                    val response = accountService.authenticate(request)
                    call.respond(response)
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(ErrorDetail("INVALID_CREDENTIALS", e.message ?: "Invalid credentials", "authentication_error"))
                    )
                }
            }
        }
    }
}
