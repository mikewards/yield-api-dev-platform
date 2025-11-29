package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.service.ApplicationService
import com.tbd.service.WalletService
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
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val request = call.receive<CreateWalletRequest>()
                    
                    // Verify application belongs to account
                    val app = applicationService.getApplication(accountId, applicationId)
                        ?: return@post call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    
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
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    
                    // Verify application belongs to account
                    val app = applicationService.getApplication(accountId, applicationId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    
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
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val walletId = UUID.fromString(call.parameters["walletId"])
                    
                    // Verify application belongs to account
                    val app = applicationService.getApplication(accountId, applicationId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    
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
                    val accountId = UUID.fromString(principal.name)
                    val applicationId = UUID.fromString(call.parameters["applicationId"])
                    val walletId = UUID.fromString(call.parameters["walletId"])
                    
                    // Verify application belongs to account
                    val app = applicationService.getApplication(accountId, applicationId)
                        ?: return@delete call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Application not found", "not_found_error"))
                        )
                    
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

