package com.flow.api.routes

import com.flow.dto.*
import com.flow.service.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.tokenRoutes() {
    val tokenService = TokenService()
    
    routing {
        route("/v1/access-tokens") {
            authenticate("bearer-auth") {
                post {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val request = call.receive<CreateTokenRequest>()
                    
                    try {
                        val (tokenId, token) = tokenService.generateToken(accountId, request.name, request.expires_in)
                        val expiresAt = request.expires_in?.let { 
                            java.time.Instant.now().plusSeconds(it.toLong())
                        }
                        val response = TokenResponse(
                            token_id = tokenId.toString(),
                            access_token = token,
                            name = request.name,
                            created_at = java.time.Instant.now().toString(),
                            expires_at = expiresAt?.toString()
                        )
                        call.respond(HttpStatusCode.Created, response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "invalid_request_error"))
                        )
                    }
                }
                
                get {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val tokens = tokenService.getTokensByAccount(accountId)
                    call.respond(TokenListResponse(tokens))
                }
                
                delete("/{tokenId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val tokenId = UUID.fromString(call.parameters["tokenId"])
                    
                    tokenService.revokeToken(tokenId)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}