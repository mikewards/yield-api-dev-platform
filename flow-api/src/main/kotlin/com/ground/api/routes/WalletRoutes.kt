package com.ground.api.routes

import com.ground.dto.*
import com.ground.middleware.CurrentUserIdKey
import com.ground.service.ApplicationService
import com.ground.service.WalletService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Application.walletRoutes() {
    val walletService = WalletService()
    val applicationService = ApplicationService()
    
    routing {
        route("/v1/applications/{applicationId}/wallets") {
            authenticate("bearer-auth") {
                // Create new wallet for application
                post {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val request = call.receive<CreateWalletRequest>()
                    
                    // Try RCAC first, then legacy
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    val app = if (userId != null) {
                        applicationService.getApplicationRcac(userId, applicationId, "write")
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.getApplication(accountId, applicationId)
                    }
                    
                    if (app == null) {
                        return@post call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                    
                    // Create wallet with same environment as application
                    val wallet = walletService.createWallet(
                        applicationId = applicationId,
                        environment = app.environment,
                        chain = request.chain,
                        label = request.label
                    )
                    
                    call.respond(
                        HttpStatusCode.Created,
                        WalletResponse(
                            wallet_id = wallet.walletId.toString(),
                            address = wallet.address,
                            environment = wallet.environment,
                            chain = wallet.chain,
                            label = wallet.label,
                            status = "active",
                            created_at = java.time.Instant.now().toString()
                        )
                    )
                }
                
                // List all wallets for application
                get {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    
                    // Try RCAC first, then legacy
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    val app = if (userId != null) {
                        applicationService.getApplicationRcac(userId, applicationId, "read")
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.getApplication(accountId, applicationId)
                    }
                    
                    if (app == null) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                    
                    val wallets = walletService.listWallets(applicationId)
                    call.respond(
                        WalletListResponse(
                            wallets = wallets.map {
                                WalletResponse(
                                    wallet_id = it.walletId.toString(),
                                    address = it.address,
                                    environment = it.environment,
                                    chain = it.chain,
                                    label = it.label,
                                    status = "active",
                                    created_at = java.time.Instant.now().toString()
                                )
                            }
                        )
                    )
                }
                
                // Get specific wallet
                get("/{walletId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val walletId = UUID.fromString(call.parameters["walletId"])
                    
                    // Try RCAC first, then legacy
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    val app = if (userId != null) {
                        applicationService.getApplicationRcac(userId, applicationId, "read")
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.getApplication(accountId, applicationId)
                    }
                    
                    if (app == null) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                    
                    val wallet = walletService.getWallet(walletId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Wallet not found", "not_found_error"))
                        )
                    
                    // Verify wallet belongs to application
                    if (wallet.walletId != walletId) {
                        return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Wallet not found", "not_found_error"))
                        )
                    }
                    
                    call.respond(
                        WalletResponse(
                            wallet_id = wallet.walletId.toString(),
                            address = wallet.address,
                            environment = wallet.environment,
                            chain = wallet.chain,
                            label = wallet.label,
                            status = "active",
                            created_at = java.time.Instant.now().toString()
                        )
                    )
                }
                
                // Archive wallet
                delete("/{walletId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val walletId = UUID.fromString(call.parameters["walletId"])
                    
                    // Try RCAC first (need admin), then legacy
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    val app = if (userId != null) {
                        applicationService.getApplicationRcac(userId, applicationId, "admin")
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        applicationService.getApplication(accountId, applicationId)
                    }
                    
                    if (app == null) {
                        return@delete call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    }
                    
                    val archived = walletService.archiveWallet(walletId)
                    if (archived) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Wallet not found", "not_found_error"))
                        )
                    }
                }
            }
        }
    }
}

