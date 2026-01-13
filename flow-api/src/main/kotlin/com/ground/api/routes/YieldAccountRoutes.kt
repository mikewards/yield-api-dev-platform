package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.YieldService
import com.ground.service.WebhookService
import com.ground.middleware.ApplicationIdKey
import com.ground.middleware.ApplicationNameKey
import com.ground.middleware.EnvironmentKey
import com.ground.middleware.CurrentUserIdKey
import com.ground.integration.ProtocolService
import com.ground.integration.morpho.MorphoMarket
import com.ground.integration.aave.AaveReserve
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    val protocolService = ProtocolService()
    
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
                        val morphoMarketsDeferred: Deferred<List<MorphoMarket>>? = if (protocolFilter == null || protocolFilter == "morpho") {
                            async { 
                                try {
                                    protocolService.listMorphoMarkets()
                                } catch (e: Exception) {
                                    println("⚠️ Morpho API error: ${e.message}")
                                    emptyList<MorphoMarket>()
                                }
                            }
                        } else null
                        
                        val aaveMarketsDeferred: Deferred<List<AaveReserve>>? = if (protocolFilter == null || protocolFilter == "aave") {
                            async {
                                try {
                                    protocolService.listAaveMarkets()
                                } catch (e: Exception) {
                                    println("⚠️ Aave API error: ${e.message}")
                                    emptyList<AaveReserve>()
                                }
                            }
                        } else null
                        
                        // Await both results (parallel execution)
                        val morphoMarkets: List<MorphoMarket> = morphoMarketsDeferred?.await() ?: emptyList()
                        val aaveMarkets: List<AaveReserve> = aaveMarketsDeferred?.await() ?: emptyList()
                        
                        // Extract rates for each currency from pre-fetched markets
                        for (curr in currencies) {
                            // Morpho rates
                            if (protocolFilter == null || protocolFilter == "morpho") {
                                val matchingMarket = morphoMarkets
                                    .filter { market -> market.loanAsset?.symbol?.equals(curr, ignoreCase = true) == true }
                                    .maxByOrNull { market -> market.state?.supplyApy ?: 0.0 }
                                
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
                                    .find { reserve -> reserve.symbol?.equals(curr, ignoreCase = true) == true }
                                
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
                    val request = call.receive<CreateYieldAccountRequest>()
                    
                    try {
                        // Try RCAC first
                        val userId = call.attributes.getOrNull(CurrentUserIdKey)
                        
                        val account = if (userId != null) {
                            yieldService.createYieldAccountRcac(userId, request)
                        } else {
                            val accountId = UUID.fromString(principal.name)
                            yieldService.createYieldAccount(accountId, request)
                        }
                        
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
                    
                    // Try RCAC first
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    
                    val accounts = if (userId != null) {
                        yieldService.listYieldAccountsRcac(userId)
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        yieldService.listYieldAccounts(accountId)
                    }
                    
                    call.respond(accounts)
                }
                
                get("/{yieldAccountId}") {
                    val principal = call.principal<UserIdPrincipal>()!!
                    val yieldAccountId = UUID.fromString(call.parameters["yieldAccountId"])
                    
                    // Try RCAC first
                    val userId = call.attributes.getOrNull(CurrentUserIdKey)
                    
                    val account = if (userId != null) {
                        yieldService.getYieldAccountRcac(userId, yieldAccountId, "read")
                    } else {
                        val accountId = UUID.fromString(principal.name)
                        yieldService.getYieldAccount(accountId, yieldAccountId)
                    }
                    
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
                    val yieldAccountId = UUID.fromString(call.parameters["yieldAccountId"])
                    val request = call.receive<DepositRequest>()
                    
                    // Get application context from auth middleware
                    val applicationId = call.attributes.getOrNull(ApplicationIdKey)
                    val applicationName = call.attributes.getOrNull(ApplicationNameKey)
                    val environment = call.attributes.getOrNull(EnvironmentKey)
                    
                    try {
                        // Try RCAC first
                        val userId = call.attributes.getOrNull(CurrentUserIdKey)
                        
                        val transaction = if (userId != null) {
                            yieldService.depositRcac(userId, yieldAccountId, request)
                        } else {
                            val accountId = UUID.fromString(principal.name)
                            yieldService.deposit(accountId, yieldAccountId, request)
                        }
                        
                        // Fire webhook event asynchronously (legacy, uses accountId if available)
                        val accountIdForWebhook = try { UUID.fromString(principal.name) } catch (e: Exception) { null }
                        if (accountIdForWebhook != null) {
                            coroutineScope {
                                launch {
                                    WebhookService.sendDepositCompleted(
                                        accountId = accountIdForWebhook,
                                        applicationId = applicationId,
                                        applicationName = applicationName,
                                        environment = environment,
                                        yieldAccountId = yieldAccountId.toString(),
                                        amount = request.amount,
                                        currency = request.currency,
                                        protocol = "auto",
                                        transactionId = transaction.transaction_id
                                    )
                                }
                            }
                        }
                        
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
                    val yieldAccountId = UUID.fromString(call.parameters["yieldAccountId"])
                    val request = call.receive<WithdrawRequest>()
                    
                    // Get application context from auth middleware
                    val applicationId = call.attributes.getOrNull(ApplicationIdKey)
                    val applicationName = call.attributes.getOrNull(ApplicationNameKey)
                    val environment = call.attributes.getOrNull(EnvironmentKey)
                    
                    try {
                        // Try RCAC first
                        val userId = call.attributes.getOrNull(CurrentUserIdKey)
                        
                        val transaction = if (userId != null) {
                            yieldService.withdrawRcac(userId, yieldAccountId, request)
                        } else {
                            val accountId = UUID.fromString(principal.name)
                            yieldService.withdraw(accountId, yieldAccountId, request)
                        }
                        
                        // Fire webhook event asynchronously (legacy, uses accountId if available)
                        val accountIdForWebhook = try { UUID.fromString(principal.name) } catch (e: Exception) { null }
                        if (accountIdForWebhook != null) {
                            coroutineScope {
                                launch {
                                    WebhookService.sendWithdrawalCompleted(
                                        accountId = accountIdForWebhook,
                                        applicationId = applicationId,
                                        applicationName = applicationName,
                                        environment = environment,
                                        yieldAccountId = yieldAccountId.toString(),
                                        amount = request.amount,
                                        currency = request.currency,
                                        transactionId = transaction.transaction_id
                                    )
                                }
                            }
                        }
                        
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