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

fun Application.marketRoutes() {
    val protocolService = ProtocolService()
    
    routing {
        route("/v1/markets") {
            authenticate("bearer-auth") {
                get {
                    val protocolFilter = call.request.queryParameters["protocol"]
                    val currencyFilter = call.request.queryParameters["currency"]
                    
                    val markets = mutableListOf<Market>()
                    
                    // Fetch Morpho markets
                    if (protocolFilter == null || protocolFilter == "morpho") {
                        try {
                            val morphoMarkets = runBlocking {
                                protocolService.listMorphoMarkets()
                            }
                            
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
                                            apy = apy,
                                            status = "active",
                                            updated_at = java.time.Instant.now().toString()
                                        ))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to fetch Morpho markets: ${e.message}")
                            // Continue with Aave markets even if Morpho fails
                        }
                    }
                    
                    // Fetch Aave markets
                    if (protocolFilter == null || protocolFilter == "aave") {
                        try {
                            val aaveMarkets = runBlocking {
                                protocolService.listAaveMarkets()
                            }
                            
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
                                            apy = apy,
                                            status = "active",
                                            updated_at = java.time.Instant.now().toString()
                                        ))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to fetch Aave markets: ${e.message}")
                            // Return whatever we have from Morpho
                        }
                    }
                    
                    // Always return a response, even if empty
                    if (markets.isEmpty()) {
                        println("⚠️ No markets found. Protocol filter: $protocolFilter, Currency filter: $currencyFilter")
                    }
                    call.respond(MarketsResponse(markets = markets))
                }
            }
        }
    }
}
