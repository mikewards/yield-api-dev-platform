package com.tbd.middleware

import com.tbd.dto.ErrorDetail
import com.tbd.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import com.tbd.middleware.captureException

fun Application.statusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    ErrorDetail(
                        code = "INVALID_REQUEST",
                        message = cause.message ?: "Invalid request",
                        type = "invalid_request_error"
                    )
                )
            )
        }
        
        exception<AuthenticationException> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    ErrorDetail(
                        code = "UNAUTHORIZED",
                        message = "Authentication required",
                        type = "authentication_error"
                    )
                )
            )
        }
        
        exception<Exception> { call, cause ->
            // Capture exception to Sentry
            captureException(cause, mapOf(
                "endpoint" to call.request.uri.path,
                "method" to call.request.httpMethod.value
            ))
            
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    ErrorDetail(
                        code = "SERVER_ERROR",
                        message = "An internal error occurred",
                        type = "server_error"
                    )
                )
            )
        }
    }
}

class AuthenticationException(message: String) : Exception(message)
