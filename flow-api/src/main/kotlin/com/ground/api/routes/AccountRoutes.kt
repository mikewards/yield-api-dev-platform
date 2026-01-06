package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.AccountService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.accountRoutes() {
    val accountService = AccountService()
    
    routing {
        route("/v1/accounts") {
            post {
                val request = call.receive<CreateAccountRequest>()
                try {
                    val account = accountService.createAccount(request)
                    call.respond(HttpStatusCode.Created, account)
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(ErrorDetail("USERNAME_TAKEN", e.message ?: "Invalid request", "invalid_request_error"))
                    )
                }
            }
            
            authenticate("bearer-auth") {
                get("/{accountId}") {
                    val accountId = UUID.fromString(call.parameters["accountId"])
                    val principal = call.principal<UserIdPrincipal>()!!
                    val authenticatedAccountId = UUID.fromString(principal.name)
                    
                    if (accountId != authenticatedAccountId) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(ErrorDetail("FORBIDDEN", "Access denied", "authorization_error"))
                        )
                        return@get
                    }
                    
                    val account = accountService.getAccount(accountId)
                    if (account != null) {
                        call.respond(account)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Account not found", "not_found_error"))
                        )
                    }
                }
            }
        }
    }
}