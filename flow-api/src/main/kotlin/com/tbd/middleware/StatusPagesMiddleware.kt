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
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ErrorResponse(
                    ErrorDetail(
                        code = "NOT_FOUND",
                        message = "Endpoint not found: ${call.request.path()}",
                        type = "not_found_error"
                    )
                )
            )
        }
        
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
            // Capture exception to Sentry (without request context to avoid API issues)
            captureException(cause)
            
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
