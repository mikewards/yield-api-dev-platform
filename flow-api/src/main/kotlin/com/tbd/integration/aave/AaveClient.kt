package com.tbd.integration.aave

import com.tbd.service.Web3Service
import com.tbd.service.WalletService
import com.tbd.service.TokenApprovalService
import com.tbd.util.retryWithBackoff
import com.tbd.util.RetryConfig
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.web3j.protocol.core.methods.response.TransactionReceipt
import java.math.BigInteger
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
data class AaveGraphQLRequest(
    val query: String
)

@Serializable
data class AaveGraphQLResponse(
    val data: AaveGraphQLData? = null,
    val errors: List<AaveGraphQLError>? = null
)

@Serializable
data class AaveGraphQLData(
    val markets: List<AaveMarket>? = null
)

@Serializable
data class AaveMarket(
    val reserves: List<AaveReserve>? = null
)

@Serializable
data class AaveReserve(
    val underlyingToken: AaveToken? = null,
    val supplyInfo: AaveSupplyInfo? = null
)

@Serializable
data class AaveToken(
    val symbol: String? = null,
    val address: String? = null
)

@Serializable
data class AaveSupplyInfo(
    val apy: AaveAPY? = null
)

@Serializable
data class AaveAPY(
    val value: String? = null
)

@Serializable
data class AaveGraphQLError(
    val message: String
)

class AaveClient {
    private val config = ConfigFactory.load()
    private val graphqlUrl = "https://api.v3.aave.com/graphql"
    private val web3Service = Web3Service()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    /**
     * Get current APY for a currency on Aave
     */
    suspend fun getCurrentRate(currency: String): Double {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 3,
                initialDelay = 500.milliseconds
            )
        ) {
            try {
                // Query Aave V3 GraphQL API for supply APY
                // Chain ID 1 = Ethereum Mainnet
                val query = """
                    query GetReserves {
                        markets(request: { chainIds: [1] }) {
                            reserves {
                                underlyingToken {
                                    symbol
                                }
                                supplyInfo {
                                    apy {
                                        value
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()
                
                val request = AaveGraphQLRequest(query = query)
                val response: AaveGraphQLResponse = client.post(graphqlUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                
                if (response.errors != null && response.errors.isNotEmpty()) {
                    throw Exception("Aave GraphQL errors: ${response.errors.joinToString { it.message }}")
                }
                
                // Find reserve matching currency (USDC, USDT, etc.)
                val reserve = response.data?.markets?.flatMap { it.reserves ?: emptyList() }
                    ?.firstOrNull { reserve ->
                        reserve.underlyingToken?.symbol?.equals(currency, ignoreCase = true) == true
                    }
                
                val apyValue = reserve?.supplyInfo?.apy?.value
                if (apyValue != null) {
                    // APY value is already in decimal format (e.g., 0.036 = 3.6%)
                    apyValue.toDoubleOrNull() ?: throw IllegalArgumentException("No active markets found for currency: $currency")
                } else {
                    throw IllegalArgumentException("No active markets found for currency: $currency")
                }
            } catch (e: IllegalArgumentException) {
                // Re-throw IllegalArgumentException so route handler can handle it
                throw e
            } catch (e: Exception) {
                // For other exceptions (network errors, etc.), throw as well
                println("⚠️ Aave API error: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * List all available reserves/markets from Aave
     */
    suspend fun listMarkets(): List<AaveReserve> {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 3,
                initialDelay = 500.milliseconds
            )
        ) {
            try {
                val query = """
                    query GetReserves {
                        markets(request: { chainIds: [1] }) {
                            reserves {
                                underlyingToken {
                                    symbol
                                    address
                                }
                                supplyInfo {
                                    apy {
                                        value
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()
                
                val request = AaveGraphQLRequest(query = query)
                val response: AaveGraphQLResponse = client.post(graphqlUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                
                if (response.errors != null && response.errors.isNotEmpty()) {
                    throw Exception("Aave GraphQL errors: ${response.errors.joinToString { it.message }}")
                }
                
                // Return all reserves with active APY
                response.data?.markets
                    ?.flatMap { it.reserves ?: emptyList() }
                    ?.filter { 
                        it.supplyInfo?.apy?.value != null && 
                        it.supplyInfo.apy.value.toDoubleOrNull() ?: 0.0 > 0.0 
                    }
                    ?: emptyList()
            } catch (e: Exception) {
                println("⚠️ Aave API error listing markets: ${e.message}")
                throw e
            }
        }
    }
    
    /**
     * Supply assets to Aave (deposit)
     * Automatically handles token approval if needed
     */
    suspend fun supply(
        walletId: UUID,
        environment: String,
        asset: String,
        amount: BigInteger,
        onBehalf: String
    ): TransactionReceipt {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 2, // Fewer retries for transactions
                initialDelay = 1.seconds
            )
        ) {
            val walletService = WalletService()
            val credentials = walletService.getCredentials(walletId)
            val web3j = web3Service.getWeb3j(environment)
            val aaveAddress = web3Service.getAaveAddress(environment)
            val gasProvider = web3Service.getGasProvider()
            
            // Ensure token approval before supply
            val approvalService = TokenApprovalService(web3j, credentials, gasProvider)
            val needsApproval = approvalService.ensureAllowance(
                tokenAddress = asset,
                spenderAddress = aaveAddress,
                requiredAmount = amount
            )
            
            if (needsApproval) {
                val approveTx = approvalService.approve(
                    tokenAddress = asset,
                    spenderAddress = aaveAddress,
                    amount = BigInteger("115792089237316195423570985008687907853269984665640564039457") // Max
                )
                approvalService.waitForReceipt(approveTx.transactionHash)
            }
            
            val wrapper = AaveContractWrapper(web3j, credentials, aaveAddress, gasProvider)
            
            val txResponse = wrapper.supply(
                asset = asset,
                amount = amount,
                onBehalfOf = onBehalf,
                referralCode = 0
            )
            
            // Wait for transaction receipt
            val receipt = web3j.ethGetTransactionReceipt(txResponse.transactionHash).send()
            receipt.transactionReceipt.get()
        }
    }
    
    /**
     * Withdraw assets from Aave
     */
    suspend fun withdraw(
        walletId: UUID,
        environment: String,
        asset: String,
        amount: BigInteger,
        to: String
    ): TransactionReceipt {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 2,
                initialDelay = 1.seconds
            )
        ) {
            val walletService = WalletService()
            val credentials = walletService.getCredentials(walletId)
            val web3j = web3Service.getWeb3j(environment)
            val aaveAddress = web3Service.getAaveAddress(environment)
            val gasProvider = web3Service.getGasProvider()
            
            val wrapper = AaveContractWrapper(web3j, credentials, aaveAddress, gasProvider)
            
            val txResponse = wrapper.withdraw(
                asset = asset,
                amount = amount,
                to = to
            )
            
            // Wait for transaction receipt
            val receipt = web3j.ethGetTransactionReceipt(txResponse.transactionHash).send()
            receipt.transactionReceipt.get()
        }
    }
    
    /**
     * Get user account data from Aave
     */
    suspend fun getUserAccountData(
        walletId: UUID,
        environment: String
    ): UserAccountData {
        val walletService = WalletService()
        val credentials = walletService.getCredentials(walletId)
        val web3j = web3Service.getWeb3j(environment)
        val aaveAddress = web3Service.getAaveAddress(environment)
        val gasProvider = web3Service.getGasProvider()
        
        val wrapper = AaveContractWrapper(web3j, credentials, aaveAddress, gasProvider)
        return wrapper.getUserAccountData(credentials.address)
    }
}
