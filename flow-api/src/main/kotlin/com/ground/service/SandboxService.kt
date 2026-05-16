package com.ground.service

import com.ground.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * Service for managing sandbox test fixtures and data
 */
class SandboxService {
    private val walletService = WalletService()
    
    /**
     * Initialize sandbox with test data for an account
     */
    fun initializeSandbox(accountId: UUID) {
        transaction {
            // Create test application
            val testAppId = Applications.insert {
                it[Applications.accountId] = accountId
                it[Applications.name] = "Sandbox Test App"
                it[Applications.description] = "Automatically created test application for sandbox environment"
                it[Applications.environment] = "both"
                it[Applications.status] = "active"
                it[Applications.sandboxRpcUrl] = "https://sepolia.infura.io/v3/test"
                it[Applications.productionRpcUrl] = "https://mainnet.infura.io/v3/test"
                it[Applications.createdAt] = Instant.now()
                it[Applications.updatedAt] = Instant.now()
            } get Applications.id
            
            // Create test wallet
            val wallet = walletService.createWallet(
                applicationId = testAppId.value,
                environment = "sandbox",
                chain = "ethereum",
                label = "Test Wallet"
            )
            
            // Create test yield account
            YieldAccounts.insert {
                it[YieldAccounts.accountId] = accountId
                it[YieldAccounts.currency] = "USDC"
                it[YieldAccounts.protocol] = "auto"
                it[YieldAccounts.status] = "active"
                it[YieldAccounts.balance] = 0.toBigDecimal()
                it[YieldAccounts.createdAt] = Instant.now()
                it[YieldAccounts.updatedAt] = Instant.now()
            }
        }
    }
    
    /**
     * Create test fixtures for common scenarios
     */
    fun createTestFixtures(accountId: UUID) {
        transaction {
            // Create multiple test applications with different currencies
            val apps = listOf(
                "E-commerce Integration" to "USDC",
                "SaaS Platform" to "USDT",
                "Mobile App" to "DAI"
            )
            
            apps.forEach { (name, currency) ->
                val appId = Applications.insert {
                    it[Applications.accountId] = accountId
                    it[Applications.name] = name
                    it[Applications.environment] = "both"
                    it[Applications.status] = "active"
                    it[Applications.sandboxRpcUrl] = "https://sepolia.infura.io/v3/test"
                    it[Applications.productionRpcUrl] = "https://mainnet.infura.io/v3/test"
                    it[Applications.createdAt] = Instant.now()
                    it[Applications.updatedAt] = Instant.now()
                } get Applications.id
                
                // Create wallet for each app
                val wallet = walletService.createWallet(
                    applicationId = appId.value,
                    environment = "sandbox",
                    chain = "ethereum",
                    label = "$name Wallet"
                )
                
                // Create yield account
                YieldAccounts.insert {
                    it[YieldAccounts.accountId] = accountId
                    it[YieldAccounts.currency] = currency
                    it[YieldAccounts.protocol] = "auto"
                    it[YieldAccounts.status] = "active"
                    it[YieldAccounts.balance] = 0.toBigDecimal()
                    it[YieldAccounts.createdAt] = Instant.now()
                    it[YieldAccounts.updatedAt] = Instant.now()
                }
            }
        }
    }
    
    /**
     * Get test token for sandbox (creates one if doesn't exist)
     */
    fun getTestToken(applicationId: UUID, environment: String = "sandbox"): String {
        return transaction {
            AccessTokens.select { 
                AccessTokens.applicationId eq applicationId and 
                (AccessTokens.environment eq environment)
            }.firstOrNull()?.let {
                it[AccessTokens.token]
            } ?: run {
                // Get accountId from application
                val accountId = Applications.select { Applications.id eq applicationId }
                    .firstOrNull()?.get(Applications.accountId)
                    ?: throw IllegalStateException("Application not found")
                
                // Create a test token
                val token = "tbd_${environment.substring(0, 4)}_${UUID.randomUUID().toString().replace("-", "")}"
                AccessTokens.insert {
                    it[AccessTokens.accountId] = accountId
                    it[AccessTokens.applicationId] = applicationId
                    it[AccessTokens.token] = token
                    it[AccessTokens.environment] = environment
                    it[AccessTokens.name] = "Sandbox Test Token"
                    it[AccessTokens.createdAt] = Instant.now()
                    it[AccessTokens.expiresAt] = null // Never expires for test tokens
                } get AccessTokens.token
            }
        }
    }
    
    /**
     * Reset sandbox data (for testing)
     */
    fun resetSandbox(accountId: UUID) {
        transaction {
            // Get all sandbox applications
            val appIds = Applications.select { 
                Applications.accountId eq accountId
            }.map { it[Applications.id].value }
            
            appIds.forEach { appId ->
                // Delete wallets
                ApplicationWallets.deleteWhere { 
                    ApplicationWallets.applicationId eq appId 
                }
                // Delete tokens
                AccessTokens.deleteWhere { 
                    AccessTokens.applicationId eq appId 
                }
            }
            
            // Delete yield accounts
            YieldAccounts.deleteWhere { 
                YieldAccounts.accountId eq accountId 
            }
            
            // Delete applications
            Applications.deleteWhere { 
                Applications.accountId eq accountId 
            }
        }
    }
}

