package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.integration.ProtocolService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// Get network name based on environment
fun getNetworkName(): String {
    val env = System.getenv("ENVIRONMENT") ?: "production"
    return if (env == "sandbox" || env == "staging") {
        "ethereum_sepolia"
    } else {
        "ethereum_mainnet"
    }
}

fun Application.marketRoutes() {
    val protocolService = ProtocolService()
    
    routing {
        route("/v1/markets") {
            authenticate("bearer-auth") {
                get {
                    try {
                        val protocolFilter = call.request.queryParameters["protocol"]
                        val currencyFilter = call.request.queryParameters["currency"]
                        val network = getNetworkName()
                        val now = java.time.Instant.now().toString()
                        
                        val markets = mutableListOf<Market>()
                        
                        // Fetch BOTH protocols in PARALLEL (2 API calls total, not sequential)
                        coroutineScope {
                            val morphoDeferred = if (protocolFilter == null || protocolFilter == "morpho") {
                                async {
                                    try {
                                        protocolService.listMorphoMarkets()
                                    } catch (e: Exception) {
                                        println("⚠️ Morpho API error: ${e.message}")
                                        emptyList()
                                    }
                                }
                            } else null
                            
                            val aaveDeferred = if (protocolFilter == null || protocolFilter == "aave") {
                                async {
                                    try {
                                        protocolService.listAaveMarkets()
                                    } catch (e: Exception) {
                                        println("⚠️ Aave API error: ${e.message}")
                                        emptyList()
                                    }
                                }
                            } else null
                            
                            // Await both in parallel
                            val morphoMarkets = morphoDeferred?.await() ?: emptyList()
                            val aaveMarkets = aaveDeferred?.await() ?: emptyList()
                            
                            // Process Morpho markets
                            morphoMarkets.forEach { morphoMarket ->
                                val symbol = morphoMarket.loanAsset?.symbol
                                val address = morphoMarket.loanAsset?.address
                                val apy = morphoMarket.state?.supplyApy ?: 0.0
                                
                                if (currencyFilter == null || symbol?.equals(currencyFilter, ignoreCase = true) == true) {
                                    if (symbol != null && apy > 0.0) {
                                        markets.add(Market(
                                            market_id = "morpho_${morphoMarket.id ?: "unknown"}",
                                            protocol = "morpho",
                                            currency = symbol,
                                            currency_address = address,
                                            network = network,
                                            apy = apy,
                                            status = "active",
                                            updated_at = now
                                        ))
                                    }
                                }
                            }
                            
                            // Process Aave markets
                            aaveMarkets.forEach { aaveReserve ->
                                val symbol = aaveReserve.symbol
                                val apy = aaveReserve.supplyApy
                                
                                if (currencyFilter == null || symbol?.equals(currencyFilter, ignoreCase = true) == true) {
                                    if (symbol != null && apy > 0.0) {
                                        markets.add(Market(
                                            market_id = "aave_${symbol.lowercase()}",
                                            protocol = "aave",
                                            currency = symbol,
                                            currency_address = aaveReserve.underlyingToken?.address,
                                            network = network,
                                            apy = apy,
                                            status = "active",
                                            updated_at = now
                                        ))
                                    }
                                }
                            }
                        }
                        
                        call.respond(MarketsResponse(markets = markets))
                    } catch (e: Exception) {
                        println("❌ Error in /v1/markets: ${e.message}")
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(
                                ErrorDetail(
                                    code = "MARKET_FETCH_ERROR",
                                    message = "Failed to fetch markets: ${e.message}",
                                    type = "server_error"
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
