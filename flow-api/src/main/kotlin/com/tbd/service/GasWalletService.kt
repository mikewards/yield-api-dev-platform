package com.tbd.service

import com.tbd.model.ApplicationWallets
import com.typesafe.config.ConfigFactory
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.*

/**
 * Service for managing gas wallet
 * A separate wallet that holds ETH to pay for transaction gas fees
 */
class GasWalletService {
    private val encryptionService = EncryptionService()
    private val config = ConfigFactory.load()
    
    /**
     * Get or create the gas wallet for an environment
     */
    fun getGasWallet(environment: String): UUID {
        return transaction {
            // Look for existing gas wallet
            val existing = ApplicationWallets.select {
                (ApplicationWallets.label eq "Gas Wallet") and 
                (ApplicationWallets.environment eq environment)
            }.firstOrNull()
            
            if (existing != null) {
                return@transaction existing[ApplicationWallets.id].value
            }
            
            // Create new gas wallet if none exists
            // Note: This should be a system-level wallet, not tied to an application
            // For now, we'll create it as a special wallet
            val keyPair = org.web3j.crypto.Keys.createEcKeyPair()
            val privateKeyHex = keyPair.privateKey.toString(16)
            val publicKey = keyPair.publicKey
            val address = "0x" + org.web3j.crypto.Keys.getAddress(publicKey)
            
            val encryptedPrivateKey = encryptionService.encrypt(privateKeyHex)
            
            val walletId = ApplicationWallets.insert {
                it[ApplicationWallets.applicationId] = UUID.randomUUID() // Placeholder - gas wallet not tied to app
                it[ApplicationWallets.address] = address
                it[ApplicationWallets.encryptedPrivateKey] = encryptedPrivateKey
                it[ApplicationWallets.chain] = "ethereum"
                it[ApplicationWallets.environment] = environment
                it[ApplicationWallets.label] = "Gas Wallet"
                it[ApplicationWallets.status] = "active"
                it[ApplicationWallets.createdAt] = java.time.Instant.now()
                it[ApplicationWallets.updatedAt] = java.time.Instant.now()
            } get ApplicationWallets.id
            
            walletId.value
        }
    }
    
    /**
     * Check gas wallet balance
     */
    fun checkBalance(web3j: Web3j, walletAddress: String): BigInteger {
        val response: EthGetBalance = web3j.ethGetBalance(walletAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send()
        return response.balance
    }
    
    /**
     * Check if gas wallet has sufficient balance
     */
    fun hasSufficientBalance(web3j: Web3j, walletAddress: String, minEth: Double = 0.01): Boolean {
        val balance = checkBalance(web3j, walletAddress)
        val balanceEth = Convert.fromWei(balance.toString(), Convert.Unit.ETHER).toDouble()
        return balanceEth >= minEth
    }
    
    /**
     * Get gas wallet address
     */
    fun getGasWalletAddress(environment: String): String {
        return transaction {
            ApplicationWallets.select {
                (ApplicationWallets.label eq "Gas Wallet") and 
                (ApplicationWallets.environment eq environment)
            }.firstOrNull()?.get(ApplicationWallets.address)
                ?: throw IllegalStateException("Gas wallet not found for environment: $environment")
        }
    }
}

