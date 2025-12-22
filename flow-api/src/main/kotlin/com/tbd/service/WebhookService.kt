package com.tbd.service

import com.svix.Svix
import com.svix.models.ApplicationIn
import com.svix.models.EndpointIn
import com.svix.models.EndpointOut
import com.svix.models.MessageIn
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Webhook Service using Svix for real-time webhook delivery
 * 
 * Event Types:
 * - deposit.completed - Funds deposited to yield account
 * - withdrawal.completed - Funds withdrawn from yield account
 * - yield.accrued - Yield has been accrued to account
 * - rate.changed - Yield rate has changed significantly
 * - account.status.changed - Account status changed (active, paused, etc.)
 */
object WebhookService {
    private val logger = LoggerFactory.getLogger(WebhookService::class.java)
    private val config = ConfigFactory.load()
    
    private val svixApiKey: String? = config.getString("svix.apiKey").takeIf { it.isNotBlank() }
    private val svix: Svix? = svixApiKey?.let { Svix(it) }
    
    // Event type constants
    object EventTypes {
        const val DEPOSIT_COMPLETED = "deposit.completed"
        const val WITHDRAWAL_COMPLETED = "withdrawal.completed"
        const val YIELD_ACCRUED = "yield.accrued"
        const val RATE_CHANGED = "rate.changed"
        const val ACCOUNT_STATUS_CHANGED = "account.status.changed"
        const val APPLICATION_CREATED = "application.created"
        const val API_KEY_CREATED = "api_key.created"
        
        val ALL = listOf(
            DEPOSIT_COMPLETED,
            WITHDRAWAL_COMPLETED,
            YIELD_ACCRUED,
            RATE_CHANGED,
            ACCOUNT_STATUS_CHANGED,
            APPLICATION_CREATED,
            API_KEY_CREATED
        )
    }
    
    /**
     * Ensure a Svix application exists for this account
     * Each TBD account maps to one Svix application
     */
    suspend fun ensureApplication(accountId: UUID): String {
        if (svix == null) {
            logger.warn("Svix not configured - webhooks disabled")
            return accountId.toString()
        }
        
        val appId = "app_$accountId"
        
        try {
            // Try to get existing app
            svix.application.get(appId)
            logger.debug("Svix application exists: $appId")
        } catch (e: Exception) {
            // Create new app if doesn't exist
            try {
                svix.application.create(ApplicationIn(
                    name = "TBD Account $accountId",
                    uid = appId
                ))
                logger.info("Created Svix application: $appId")
            } catch (createError: Exception) {
                logger.error("Failed to create Svix application: ${createError.message}")
            }
        }
        
        return appId
    }
    
    /**
     * Register a webhook endpoint for an account
     */
    suspend fun createEndpoint(
        accountId: UUID,
        url: String,
        description: String? = null,
        filterTypes: List<String>? = null
    ): EndpointOut? {
        if (svix == null) {
            logger.warn("Svix not configured - cannot create endpoint")
            return null
        }
        
        val appId = ensureApplication(accountId)
        
        return try {
            val endpoint = svix.endpoint.create(
                appId,
                EndpointIn(
                    url = url,
                    description = description,
                    filterTypes = filterTypes?.toSet()
                )
            )
            logger.info("Created webhook endpoint for account $accountId: ${endpoint.id}")
            endpoint
        } catch (e: Exception) {
            logger.error("Failed to create webhook endpoint: ${e.message}")
            null
        }
    }
    
    /**
     * List all webhook endpoints for an account
     */
    suspend fun listEndpoints(accountId: UUID): List<EndpointOut> {
        if (svix == null) {
            return emptyList()
        }
        
        val appId = "app_$accountId"
        
        return try {
            svix.endpoint.list(appId).data ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to list endpoints: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Get a specific endpoint
     */
    suspend fun getEndpoint(accountId: UUID, endpointId: String): EndpointOut? {
        if (svix == null) return null
        
        val appId = "app_$accountId"
        
        return try {
            svix.endpoint.get(appId, endpointId)
        } catch (e: Exception) {
            logger.error("Failed to get endpoint: ${e.message}")
            null
        }
    }
    
    /**
     * Delete a webhook endpoint
     */
    suspend fun deleteEndpoint(accountId: UUID, endpointId: String): Boolean {
        if (svix == null) return false
        
        val appId = "app_$accountId"
        
        return try {
            svix.endpoint.delete(appId, endpointId)
            logger.info("Deleted webhook endpoint: $endpointId")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete endpoint: ${e.message}")
            false
        }
    }
    
    /**
     * Send a webhook event
     */
    suspend fun sendEvent(
        accountId: UUID,
        eventType: String,
        payload: Map<String, Any?>
    ): Boolean {
        if (svix == null) {
            logger.debug("Svix not configured - skipping webhook for event: $eventType")
            return false
        }
        
        val appId = "app_$accountId"
        val eventId = "evt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        
        return try {
            svix.message.create(
                appId,
                MessageIn(
                    eventType = eventType,
                    eventId = eventId,
                    payload = payload
                )
            )
            logger.info("Sent webhook event: $eventType to account $accountId")
            true
        } catch (e: Exception) {
            logger.error("Failed to send webhook event: ${e.message}")
            false
        }
    }
    
    // Convenience methods for specific events
    
    suspend fun sendDepositCompleted(
        accountId: UUID,
        yieldAccountId: String,
        amount: String,
        currency: String,
        protocol: String,
        transactionId: String? = null
    ) = sendEvent(
        accountId,
        EventTypes.DEPOSIT_COMPLETED,
        mapOf(
            "yield_account_id" to yieldAccountId,
            "amount" to amount,
            "currency" to currency,
            "protocol" to protocol,
            "transaction_id" to transactionId,
            "timestamp" to System.currentTimeMillis()
        )
    )
    
    suspend fun sendWithdrawalCompleted(
        accountId: UUID,
        yieldAccountId: String,
        amount: String,
        currency: String,
        transactionId: String? = null
    ) = sendEvent(
        accountId,
        EventTypes.WITHDRAWAL_COMPLETED,
        mapOf(
            "yield_account_id" to yieldAccountId,
            "amount" to amount,
            "currency" to currency,
            "transaction_id" to transactionId,
            "timestamp" to System.currentTimeMillis()
        )
    )
    
    suspend fun sendYieldAccrued(
        accountId: UUID,
        yieldAccountId: String,
        yieldAmount: String,
        totalBalance: String,
        currency: String,
        apy: Double
    ) = sendEvent(
        accountId,
        EventTypes.YIELD_ACCRUED,
        mapOf(
            "yield_account_id" to yieldAccountId,
            "yield_amount" to yieldAmount,
            "total_balance" to totalBalance,
            "currency" to currency,
            "apy" to apy,
            "timestamp" to System.currentTimeMillis()
        )
    )
    
    suspend fun sendRateChanged(
        accountId: UUID,
        currency: String,
        protocol: String,
        oldRate: Double,
        newRate: Double
    ) = sendEvent(
        accountId,
        EventTypes.RATE_CHANGED,
        mapOf(
            "currency" to currency,
            "protocol" to protocol,
            "old_rate" to oldRate,
            "new_rate" to newRate,
            "change_percent" to ((newRate - oldRate) / oldRate * 100),
            "timestamp" to System.currentTimeMillis()
        )
    )
    
    suspend fun sendAccountStatusChanged(
        accountId: UUID,
        yieldAccountId: String,
        oldStatus: String,
        newStatus: String
    ) = sendEvent(
        accountId,
        EventTypes.ACCOUNT_STATUS_CHANGED,
        mapOf(
            "yield_account_id" to yieldAccountId,
            "old_status" to oldStatus,
            "new_status" to newStatus,
            "timestamp" to System.currentTimeMillis()
        )
    )
    
    /**
     * Check if webhooks are enabled (Svix configured)
     */
    fun isEnabled(): Boolean = svix != null
}

