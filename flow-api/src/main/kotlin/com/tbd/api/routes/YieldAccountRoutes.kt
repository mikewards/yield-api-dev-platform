package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.service.YieldService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlinx.coroutines.runBlocking

fun Application.yieldAccountRoutes() {
    val yieldService = YieldService()
    val protocolService = com.tbd.integration.ProtocolService()
    
    routing {
        route("/v1/yield/rates") {
            authenticate("bearer-auth") {
                get {
                    val currency = call.request.queryParameters["currency"]
                    val protocolFilter = call.request.queryParameters["protocol"]
                    
                    try {
                        val currencies = if (currency != null) {
                            listOf(currency.uppercase())
                        } else {
                            listOf("USDC", "USDT", "DAI", "ETH", "WBTC")
                        }
                        
                        val rates = mutableListOf<Map<String, Any>>()
                        
                        for (curr in currencies) {
                            if (protocolFilter == null || protocolFilter == "morpho") {
                                val morphoRate = kotlinx.coroutines.runBlocking {
                                    protocolService.getMorphoRates(curr)
                                }
                                rates.add(mapOf(
                                    "currency" to curr,
                                    "protocol" to "morpho",
                                    "annual_yield_rate" to 0.06,
                                    "apy" to morphoRate,
                                    "updated_at" to java.time.Instant.now().toString()
                                ))
                            }
                            
                            if (protocolFilter == null || protocolFilter == "aave") {
                                val aaveRate = kotlinx.coroutines.runBlocking {
                                    protocolService.getAaveRates(curr)
                                }
                                rates.add(mapOf(
                                    "currency" to curr,
                                    "protocol" to "aave",
                                    "annual_yield_rate" to 0.06,
                                    "apy" to aaveRate,
                                    "updated_at" to java.time.Instant.now().toString()
                                ))
                            }
                        }
                        
                        call.respond(mapOf("rates" to rates))
                    } catch (e: Exception) {
                        println("Error getting yield rates: ${e.message}")
                        e.printStackTrace()
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(ErrorDetail("INTERNAL_ERROR", "Failed to fetch yield rates", "internal_error"))
                        )
                    }
                }
            }
        }
        
        route("/v1/yield/accounts") {
            authenticate("bearer-auth") {
                post {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val request = call.receive<CreateYieldAccountRequest>()
                    
                    try {
                        val account = yieldService.createYieldAccount(accountId, request)
                        call.respond(HttpStatusCode.Created, account)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "invalid_request_error"))
                        )
                    }
                }
                
                get {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val accounts = yieldService.listYieldAccounts(accountId)
                    call.respond(accounts)
                }
                
                get("/{yieldAccountId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val yieldAccountId = UUID.fromString(call.parameters["yieldAccountId"])
                    
                    val account = yieldService.getYieldAccount(accountId, yieldAccountId)
                    if (account != null) {
                        call.respond(account)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Yield account not found", "not_found_error"))
                        )
                    }
                }
                
                post("/{yieldAccountId}/deposit") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val yieldAccountId = UUID.fromString(call.parameters["yieldAccountId"])
                    val request = call.receive<DepositRequest>()
                    
                    try {
                        val transaction = yieldService.deposit(accountId, yieldAccountId, request)
                        call.respond(HttpStatusCode.Created, transaction)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_CURRENCY", e.message ?: "Invalid request", "invalid_request_error"))
                        )
                    }
                }
                
                post("/{yieldAccountId}/withdraw") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val accountId = UUID.fromString(principal.name)
                    val yieldAccountId = UUID.fromString(call.parameters["yieldAccountId"])
                    val request = call.receive<WithdrawRequest>()
                    
                    try {
                        val transaction = yieldService.withdraw(accountId, yieldAccountId, request)
                        call.respond(HttpStatusCode.Created, transaction)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INSUFFICIENT_BALANCE", e.message ?: "Invalid request", "invalid_request_error"))
                        )
                    }
                }
            }
        }
    }
}