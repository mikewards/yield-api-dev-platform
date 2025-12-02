package com.tbd.api.routes

import com.tbd.dto.*
import com.tbd.integration.ProtocolService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

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
                    println("🔐 /v1/markets endpoint called - authentication passed")
                    try {
                        val protocolFilter = call.request.queryParameters["protocol"]
                        val currencyFilter = call.request.queryParameters["currency"]
                        val network = getNetworkName()
                        
                        println("📊 Fetching markets - Protocol: $protocolFilter, Currency: $currencyFilter, Network: $network")
                        
                        val markets = mutableListOf<Market>()
                    
                    // Fetch Morpho markets
                    if (protocolFilter == null || protocolFilter == "morpho") {
                        try {
                            println("🔄 Fetching Morpho markets...")
                            val morphoMarkets = runBlocking {
                                protocolService.listMorphoMarkets()
                            }
                            println("✅ Morpho markets fetched: ${morphoMarkets.size} markets")
                            
                            morphoMarkets.forEach { morphoMarket ->
                                val symbol = morphoMarket.loanAsset?.symbol
                                val address = morphoMarket.loanAsset?.address
                                val apy = morphoMarket.state?.supplyApy ?: 0.0
                                
                                // Apply currency filter if specified
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
                                            updated_at = java.time.Instant.now().toString()
                                        ))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to fetch Morpho markets: ${e.message}")
                            e.printStackTrace()
                            // Continue with Aave markets even if Morpho fails
                        }
                    }
                    
                    // Fetch Aave markets
                    if (protocolFilter == null || protocolFilter == "aave") {
                        try {
                            println("🔄 Fetching Aave markets...")
                            val aaveMarkets = runBlocking {
                                protocolService.listAaveMarkets()
                            }
                            println("✅ Aave markets fetched: ${aaveMarkets.size} markets")
                            
                            aaveMarkets.forEach { aaveReserve ->
                                val symbol = aaveReserve.underlyingToken?.symbol
                                val apyValue = aaveReserve.supplyInfo?.apy?.value
                                val apy = apyValue?.toDoubleOrNull() ?: 0.0
                                
                                // Apply currency filter if specified
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
                                            updated_at = java.time.Instant.now().toString()
                                        ))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to fetch Aave markets: ${e.message}")
                            e.printStackTrace()
                            // Return whatever we have from Morpho
                        }
                    }
                    
                        // Always return a response, even if empty
                        println("📦 Total markets collected: ${markets.size}")
                        if (markets.isEmpty()) {
                            println("⚠️ No markets found. Protocol filter: $protocolFilter, Currency filter: $currencyFilter")
                        } else {
                            println("✅ Returning ${markets.size} markets")
                        }
                        
                        // Ensure we always send a response
                        val response = MarketsResponse(markets = markets)
                        println("📤 Sending response: ${response.markets.size} markets")
                        call.respond(response)
                    } catch (e: Exception) {
                        println("❌ Error in /v1/markets: ${e.message}")
                        e.printStackTrace()
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
