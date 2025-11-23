package com.tbd.integration.morpho

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
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.crypto.Credentials
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
data class MorphoGraphQLRequest(
    val query: String,
    val variables: Map<String, String>? = null
)

@Serializable
data class MorphoGraphQLResponse(
    val data: MorphoData? = null,
    val errors: List<MorphoError>? = null
)

@Serializable
data class MorphoData(
    val markets: MorphoMarketsResponse? = null
)

@Serializable
data class MorphoMarketsResponse(
    val items: List<MorphoMarket>? = null
)

@Serializable
data class MorphoMarket(
    val id: String? = null,
    val loanAsset: MorphoAsset? = null,
    val state: MorphoMarketState? = null
)

@Serializable
data class MorphoAsset(
    val symbol: String? = null,
    val address: String? = null
)

@Serializable
data class MorphoMarketState(
    val supplyApy: Double? = null
)

@Serializable
data class MorphoError(
    val message: String
)

class MorphoClient {
    private val config = ConfigFactory.load()
    private val graphqlUrl = "https://blue-api.morpho.org/graphql"
    private val web3Service = Web3Service()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    /**
     * Get current APY for a currency on Morpho
     */
    suspend fun getCurrentRate(currency: String): Double {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 3,
                initialDelay = 500.milliseconds
            )
        ) {
            try {
                val query = """
                    query GetMarkets {
                        markets {
                            items {
                                id
                                loanAsset {
                                    symbol
                                    address
                                }
                                state {
                                    supplyApy
                                }
                            }
                        }
                    }
                """.trimIndent()
                
                val request = MorphoGraphQLRequest(query = query)
                val response: MorphoGraphQLResponse = client.post(graphqlUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
                
                // Find market matching currency (USDC, USDT, etc.)
                val market = response.data?.markets?.items?.firstOrNull { market ->
                    market.loanAsset?.symbol?.equals(currency, ignoreCase = true) == true ||
                    market.loanAsset?.address?.contains(currency, ignoreCase = true) == true
                }
                
                // supplyApy is already a decimal (e.g., 0.06 for 6%), not a percentage
                // It's already a Double?, so we just need to handle null
                market?.state?.supplyApy ?: 0.06 // Default 6% if not found
            } catch (e: Exception) {
                println("⚠️ Morpho API error: ${e.message}")
                0.06 // Default 6% if all retries fail
            }
        }
    }
    
    /**
     * Supply assets to Morpho (deposit)
     * Automatically handles token approval if needed
     */
    suspend fun supply(
        walletId: UUID,
        environment: String,
        marketParams: MarketParams,
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
            val morphoAddress = web3Service.getMorphoAddress(environment)
            val gasProvider = web3Service.getGasProvider()
            
            // Ensure token approval before supply
            val approvalService = TokenApprovalService(web3j, credentials, gasProvider)
            val needsApproval = approvalService.ensureAllowance(
                tokenAddress = marketParams.loanToken,
                spenderAddress = morphoAddress,
                requiredAmount = amount
            )
            
            if (needsApproval) {
                val approveTx = approvalService.approve(
                    tokenAddress = marketParams.loanToken,
                    spenderAddress = morphoAddress,
                    amount = BigInteger("115792089237316195423570985008687907853269984665640564039457") // Max
                )
                approvalService.waitForReceipt(approveTx.transactionHash)
            }
            
            val wrapper = MorphoContractWrapper(web3j, credentials, morphoAddress, gasProvider)
            
            // Supply with shares = 0 (let Morpho calculate)
            val txResponse = wrapper.supply(
                marketParams = marketParams,
                assets = amount,
                shares = BigInteger.ZERO,
                onBehalf = onBehalf,
                data = ByteArray(0)
            )
            
            // Wait for transaction receipt
            val receipt = web3j.ethGetTransactionReceipt(txResponse.transactionHash).send()
            receipt.transactionReceipt.get()
        }
    }
    
    /**
     * Withdraw assets from Morpho
     */
    suspend fun withdraw(
        walletId: UUID,
        environment: String,
        marketParams: MarketParams,
        assets: BigInteger,
        shares: BigInteger,
        receiver: String
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
            val morphoAddress = web3Service.getMorphoAddress(environment)
            val gasProvider = web3Service.getGasProvider()
            
            val wrapper = MorphoContractWrapper(web3j, credentials, morphoAddress, gasProvider)
            
            val txResponse = wrapper.withdraw(
                marketParams = marketParams,
                assets = assets,
                shares = shares,
                onBehalf = receiver,
                receiver = receiver,
                data = ByteArray(0)
            )
            
            // Wait for transaction receipt
            val receipt = web3j.ethGetTransactionReceipt(txResponse.transactionHash).send()
            receipt.transactionReceipt.get()
        }
    }
    
    /**
     * Get position for a wallet in a market
     */
    suspend fun getPosition(
        walletId: UUID,
        environment: String,
        marketParams: MarketParams
    ): Position {
        val walletService = WalletService()
        val credentials = walletService.getCredentials(walletId)
        val web3j = web3Service.getWeb3j(environment)
        val morphoAddress = web3Service.getMorphoAddress(environment)
        val gasProvider = web3Service.getGasProvider()
        
        val wrapper = MorphoContractWrapper(web3j, credentials, morphoAddress, gasProvider)
        return wrapper.getPosition(marketParams, credentials.address)
    }
}
