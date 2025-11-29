package com.tbd.integration.aave

import com.tbd.service.Web3Service
import com.tbd.service.WalletService
import com.tbd.service.TokenApprovalService
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

@Serializable
data class AaveReserveData(
    val symbol: String? = null,
    val liquidityRate: String? = null,
    val variableBorrowRate: String? = null,
    val stableBorrowRate: String? = null,
    val totalLiquidity: String? = null
)

@Serializable
data class AaveMarket(
    val currency: String,
    val apy: Double,
    val tvl: String
)

class AaveClient {
    private val config = ConfigFactory.load()
    private val apiUrl = "https://aave-api-v2.aave.com"
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
        try {
            // Aave API endpoint for reserve data
            // Note: This is a simplified version - actual Aave API may require different endpoints
            val response: Map<String, AaveReserveData> = client.get("$apiUrl/reserves") {
                contentType(ContentType.Application.Json)
            }.body()
            
            // Find reserve matching currency
            val reserve = response.values.firstOrNull { 
                it.symbol?.equals(currency, ignoreCase = true) == true 
            }
            
            // Convert liquidity rate from Ray (27 decimals) to percentage
            val liquidityRate = reserve?.liquidityRate?.toBigIntegerOrNull()
            return if (liquidityRate != null) {
                // Aave rates are in Ray (1e27), convert to percentage
                liquidityRate.toDouble() / 1e27 * 100.0
            } else {
                0.06 // Default 6% if not found
            }
        } catch (e: Exception) {
            // Fallback to default rate on error
            return 0.06
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
        return receipt.transactionReceipt.get()
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
        return receipt.transactionReceipt.get()
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
