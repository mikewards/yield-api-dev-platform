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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// Get network name based on environment
fun getYieldNetworkName(): String {
    val env = System.getenv("ENVIRONMENT") ?: "production"
    return if (env == "sandbox" || env == "staging") {
        "ethereum_sepolia"
    } else {
        "ethereum_mainnet"
    }
}

fun Application.yieldAccountRoutes() {
    val yieldService = YieldService()
    val protocolService = com.tbd.integration.ProtocolService()
    
    routing {
        route("/v1/yield/rates") {
            authenticate("bearer-auth") {
                get {
                    val currency = call.request.queryParameters["currency"]
                    val protocolFilter = call.request.queryParameters["protocol"]
                    val network = getYieldNetworkName()
                    
                    val currencies = if (currency != null) {
                        listOf(currency.uppercase())
                    } else {
                        listOf("USDC", "USDT", "DAI", "ETH", "WBTC")
                    }
                    
                    val rates = mutableListOf<YieldRate>()
                    val now = java.time.Instant.now().toString()
                    
                    // Fetch ALL markets from both protocols in PARALLEL (2 API calls total)
                    coroutineScope {
                        val morphoMarketsDeferred = if (protocolFilter == null || protocolFilter == "morpho") {
                            async { 
                                try {
                                    protocolService.listMorphoMarkets()
                                } catch (e: Exception) {
                                    println("⚠️ Morpho API error: ${e.message}")
                                    emptyList()
                                }
                            }
                        } else null
                        
                        val aaveMarketsDeferred = if (protocolFilter == null || protocolFilter == "aave") {
                            async {
                                try {
                                    protocolService.listAaveMarkets()
                                } catch (e: Exception) {
                                    println("⚠️ Aave API error: ${e.message}")
                                    emptyList()
                                }
                            }
                        } else null
                        
                        // Await both results (parallel execution)
                        val morphoMarkets = morphoMarketsDeferred?.await() ?: emptyList()
                        val aaveMarkets = aaveMarketsDeferred?.await() ?: emptyList()
                        
                        // Extract rates for each currency from pre-fetched markets
                        for (curr in currencies) {
                            // Morpho rates
                            if (protocolFilter == null || protocolFilter == "morpho") {
                                val matchingMarket = morphoMarkets
                                    .filter { it.loanAsset?.symbol?.equals(curr, ignoreCase = true) == true }
                                    .maxByOrNull { it.state?.supplyApy ?: 0.0 }
                                
                                val apy = matchingMarket?.state?.supplyApy ?: 0.0
                                rates.add(YieldRate(
                                    currency = curr,
                                    protocol = "morpho",
                                    network = network,
                                    annual_yield_rate = apy,
                                    apy = apy,
                                    updated_at = now,
                                    note = if (apy == 0.0) "No active markets" else null
                                ))
                            }
                            
                            // Aave rates
                            if (protocolFilter == null || protocolFilter == "aave") {
                                val matchingReserve = aaveMarkets
                                    .find { it.symbol?.equals(curr, ignoreCase = true) == true }
                                
                                val apy = matchingReserve?.supplyApy ?: 0.0
                                rates.add(YieldRate(
                                    currency = curr,
                                    protocol = "aave",
                                    network = network,
                                    annual_yield_rate = apy,
                                    apy = apy,
                                    updated_at = now,
                                    note = if (apy == 0.0) "No active markets" else null
                                ))
                            }
                        }
                    }
                    
                    call.respond(YieldRatesResponse(rates = rates))
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