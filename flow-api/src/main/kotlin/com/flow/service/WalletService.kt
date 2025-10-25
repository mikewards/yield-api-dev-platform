package com.flow.service

import com.flow.model.ApplicationWallets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.time.Instant
import java.util.*

data class WalletInfo(
    val walletId: UUID,
    val address: String,
    val environment: String,
    val chain: String,
    val label: String?
)

class WalletService {
    private val encryptionService = EncryptionService()
    
    /**
     * Create a new Ethereum wallet for an application
     */
    fun createWallet(
        applicationId: UUID,
        environment: String,
        chain: String = "ethereum",
        label: String? = null
    ): WalletInfo {
        return transaction {
            // Generate new Ethereum keypair
            val keyPair = Keys.createEcKeyPair()
            val privateKeyHex = keyPair.privateKey.toString(16)
            val publicKey = keyPair.publicKey
            val address = "0x" + Keys.getAddress(publicKey)
            
            // Encrypt private key
            val encryptedPrivateKey = encryptionService.encrypt(privateKeyHex)
            
            // Store wallet
            val now = Instant.now()
            val walletId = ApplicationWallets.insert {
                it[ApplicationWallets.applicationId] = applicationId
                it[ApplicationWallets.address] = address
                it[ApplicationWallets.encryptedPrivateKey] = encryptedPrivateKey
                it[ApplicationWallets.chain] = chain
                it[ApplicationWallets.environment] = environment
                it[ApplicationWallets.label] = label
                it[ApplicationWallets.status] = "active"
                it[ApplicationWallets.createdAt] = now
                it[ApplicationWallets.updatedAt] = now
            } get ApplicationWallets.id
            
            WalletInfo(
                walletId = walletId.value,
                address = address,
                environment = environment,
                chain = chain,
                label = label
            )
        }
    }
    
    /**
     * Get decrypted private key for a wallet (use carefully!)
     */
    fun getPrivateKey(walletId: UUID): String {
        return transaction {
            val wallet = ApplicationWallets.select { ApplicationWallets.id eq walletId }
                .firstOrNull() ?: throw IllegalArgumentException("Wallet not found")
            
            val encrypted = wallet[ApplicationWallets.encryptedPrivateKey]
            encryptionService.decrypt(encrypted)
        }
    }
    
    /**
     * Get wallet credentials for Web3j
     */
    fun getCredentials(walletId: UUID): Credentials {
        val privateKeyHex = getPrivateKey(walletId)
        return Credentials.create(privateKeyHex)
    }
    
    /**
     * List all wallets for an application
     */
    fun listWallets(applicationId: UUID): List<WalletInfo> {
        return transaction {
            ApplicationWallets.select { ApplicationWallets.applicationId eq applicationId }
                .map {
                    WalletInfo(
                        walletId = it[ApplicationWallets.id].value,
                        address = it[ApplicationWallets.address],
                        environment = it[ApplicationWallets.environment],
                        chain = it[ApplicationWallets.chain],
                        label = it[ApplicationWallets.label]
                    )
                }
        }
    }
    
    /**
     * Get wallet by ID
     */
    fun getWallet(walletId: UUID): WalletInfo? {
        return transaction {
            ApplicationWallets.select { ApplicationWallets.id eq walletId }
                .firstOrNull()?.let {
                    WalletInfo(
                        walletId = it[ApplicationWallets.id].value,
                        address = it[ApplicationWallets.address],
                        environment = it[ApplicationWallets.environment],
                        chain = it[ApplicationWallets.chain],
                        label = it[ApplicationWallets.label]
                    )
                }
        }
    }
    
    /**
     * Archive a wallet (soft delete)
     */
    fun archiveWallet(walletId: UUID): Boolean {
        return transaction {
            val updated = ApplicationWallets.update({ ApplicationWallets.id eq walletId }) {
                it[ApplicationWallets.status] = "archived"
                it[ApplicationWallets.updatedAt] = Instant.now()
            }
            updated > 0
        }
    }
}

