package com.tbd.service

import com.svix.EndpointListOptions
import com.svix.Svix
import com.svix.models.ApplicationIn
import com.svix.models.EndpointIn
import com.svix.models.EndpointOut
import com.svix.models.EventTypeIn
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
    
    private val svixApiKey: String? = try {
        val key = config.getString("svix.apiKey").takeIf { it.isNotBlank() }
        if (key != null) {
            logger.info("Svix API key configured (length: ${key.length}, prefix: ${key.take(10)}...)")
        } else {
            logger.warn("Svix API key is blank or empty")
        }
        key
    } catch (e: Exception) {
        logger.warn("Svix API key not found in config: ${e.message}")
        // Try direct environment variable as fallback
        val envKey = System.getenv("SVIX_API_KEY")?.takeIf { it.isNotBlank() }
        if (envKey != null) {
            logger.info("Using SVIX_API_KEY from environment (length: ${envKey.length})")
        } else {
            logger.error("SVIX_API_KEY not found in environment either - webhooks will be disabled")
        }
        envKey
    }
    private val svix: Svix? = svixApiKey?.let { 
        try {
            val client = Svix(it)
            logger.info("Svix client initialized successfully")
            client
        } catch (e: Exception) {
            logger.error("Failed to initialize Svix client: ${e.message}")
            null
        }
    }
    
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
     * Check if webhook service is available
     */
    fun isAvailable(): Boolean = svix != null
    
    /**
     * Get webhook service status for debugging
     */
    fun getStatus(): Map<String, Any> = mapOf(
        "available" to (svix != null),
        "apiKeyConfigured" to (svixApiKey != null),
        "apiKeyLength" to (svixApiKey?.length ?: 0)
    )
    
    /**
     * Initialize Svix event types - must be called on startup
     */
    fun initializeEventTypes() {
        if (svix == null) {
            logger.warn("Svix not configured - skipping event type registration")
            return
        }
        
        val eventTypeDescriptions = mapOf(
            EventTypes.DEPOSIT_COMPLETED to "Triggered when funds are successfully deposited to a yield account",
            EventTypes.WITHDRAWAL_COMPLETED to "Triggered when funds are successfully withdrawn from a yield account",
            EventTypes.YIELD_ACCRUED to "Triggered when yield is accrued to an account (daily)",
            EventTypes.RATE_CHANGED to "Triggered when yield rates change significantly (>0.5%)",
            EventTypes.ACCOUNT_STATUS_CHANGED to "Triggered when a yield account status changes",
            EventTypes.APPLICATION_CREATED to "Triggered when a new application is created",
            EventTypes.API_KEY_CREATED to "Triggered when a new API key is generated"
        )
        
        eventTypeDescriptions.forEach { (eventType, description) ->
            try {
                val eventTypeIn = EventTypeIn()
                eventTypeIn.name = eventType
                eventTypeIn.description = description
                svix.eventType.create(eventTypeIn)
                logger.info("Registered Svix event type: $eventType")
            } catch (e: Exception) {
                // Event type might already exist - that's fine
                if (e.message?.contains("409") == true || e.message?.contains("already exists") == true) {
                    logger.debug("Event type already exists: $eventType")
                } else {
                    logger.warn("Failed to register event type $eventType: ${e.message}")
                }
            }
        }
        
        logger.info("Svix event types initialization complete")
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
                val appIn = ApplicationIn()
                appIn.name = "TBD Account $accountId"
                appIn.uid = appId
                svix.application.create(appIn)
                logger.info("Created Svix application: $appId")
            } catch (createError: Exception) {
                logger.error("Failed to create Svix application: ${createError.message}")
            }
        }
        
        return appId
    }
    
    /**
     * Result class for endpoint creation
     */
    data class CreateEndpointResult(
        val endpoint: EndpointOut? = null,
        val error: String? = null
    )
    
    /**
     * Register a webhook endpoint for an account
     */
    suspend fun createEndpoint(
        accountId: UUID,
        url: String,
        description: String? = null,
        filterTypes: List<String>? = null
    ): CreateEndpointResult {
        if (svix == null) {
            logger.warn("Svix not configured - cannot create endpoint")
            return CreateEndpointResult(error = "Svix not configured")
        }
        
        val appId = ensureApplication(accountId)
        logger.info("createEndpoint: Using appId=$appId for accountId=$accountId")
        
        return try {
            val endpointIn = EndpointIn()
            endpointIn.url = java.net.URI.create(url)
            // Only set description if it's not null/blank - Svix doesn't accept null
            if (!description.isNullOrBlank()) {
                endpointIn.description = description
            }
            if (filterTypes != null && filterTypes.isNotEmpty()) {
                endpointIn.filterTypes = filterTypes.toSet()
            }
            
            logger.info("createEndpoint: Creating endpoint with url=$url, filterTypes=$filterTypes")
            val endpoint = svix.endpoint.create(appId, endpointIn)
            logger.info("createEndpoint: SUCCESS - Created endpoint ${endpoint.id} for appId=$appId")
            CreateEndpointResult(endpoint = endpoint)
        } catch (e: Exception) {
            logger.error("Failed to create webhook endpoint: ${e.message}", e)
            CreateEndpointResult(error = e.message ?: "Unknown error creating endpoint")
        }
    }
    
    /**
     * Debug method to see raw Svix response
     */
    fun debugListEndpoints(accountId: UUID): String {
        if (svix == null) {
            return "Svix not configured"
        }
        
        val appId = "app_$accountId"
        
        return try {
            // First check if app exists
            val app = try {
                svix.application.get(appId)
            } catch (e: Exception) {
                return "App not found: ${e.message}"
            }
            
            val appInfo = "App found: id=${app.id}, uid=${app.uid}, name=${app.name}"
            
            // Now try to list endpoints with empty options
            val options = EndpointListOptions()
            val result = svix.endpoint.list(appId, options)
            val endpoints = result.data ?: emptyList()
            val hasMore = result.iterator != null
            
            val endpointInfo = endpoints.map { "id=${it.id}, url=${it.url}" }.joinToString("; ")
            
            "$appInfo | Endpoints: count=${endpoints.size}, hasMore=$hasMore, data=[$endpointInfo]"
        } catch (e: Exception) {
            "Error: ${e.javaClass.simpleName}: ${e.message}"
        }
    }
    
    /**
     * List all webhook endpoints for an account
     */
    suspend fun listEndpoints(accountId: UUID): List<EndpointOut> {
        if (svix == null) {
            logger.warn("listEndpoints: Svix not configured")
            return emptyList()
        }
        
        // Use the same appId format as createEndpoint
        val appId = "app_$accountId"
        logger.info("listEndpoints: Looking up endpoints for accountId=$accountId, appId=$appId")
        
        return try {
            val options = EndpointListOptions()
            val result = svix.endpoint.list(appId, options)
            val endpoints = result.data ?: emptyList()
            logger.info("listEndpoints: Found ${endpoints.size} endpoints for appId=$appId")
            endpoints.forEach { ep ->
                logger.info("  - Endpoint: id=${ep.id}, url=${ep.url}")
            }
            endpoints
        } catch (e: Exception) {
            // If app doesn't exist, that's ok - just return empty
            if (e.message?.contains("404") == true || e.message?.contains("not found") == true) {
                logger.info("listEndpoints: No Svix app found for $appId - returning empty list")
                return emptyList()
            }
            logger.error("Failed to list endpoints for appId=$appId: ${e.message}", e)
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
            val messageIn = MessageIn()
            messageIn.eventType = eventType
            messageIn.eventId = eventId
            messageIn.payload = payload
            
            svix.message.create(appId, messageIn)
            logger.info("Sent webhook event: $eventType to account $accountId")
            true
        } catch (e: Exception) {
            logger.error("Failed to send webhook event: ${e.message}")
            false
        }
    }
    
    // Convenience methods for specific events
    // All methods now include applicationId and applicationName for traceability
    
    suspend fun sendDepositCompleted(
        accountId: UUID,
        applicationId: String?,
        applicationName: String?,
        environment: String?,
        yieldAccountId: String,
        amount: String,
        currency: String,
        protocol: String,
        transactionId: String? = null
    ) = sendEvent(
        accountId,
        EventTypes.DEPOSIT_COMPLETED,
        mapOf(
            "application_id" to applicationId,
            "application_name" to applicationName,
            "environment" to environment,
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
        applicationId: String?,
        applicationName: String?,
        environment: String?,
        yieldAccountId: String,
        amount: String,
        currency: String,
        transactionId: String? = null
    ) = sendEvent(
        accountId,
        EventTypes.WITHDRAWAL_COMPLETED,
        mapOf(
            "application_id" to applicationId,
            "application_name" to applicationName,
            "environment" to environment,
            "yield_account_id" to yieldAccountId,
            "amount" to amount,
            "currency" to currency,
            "transaction_id" to transactionId,
            "timestamp" to System.currentTimeMillis()
        )
    )
    
    suspend fun sendYieldAccrued(
        accountId: UUID,
        applicationId: String?,
        applicationName: String?,
        environment: String?,
        yieldAccountId: String,
        yieldAmount: String,
        totalBalance: String,
        currency: String,
        apy: Double
    ) = sendEvent(
        accountId,
        EventTypes.YIELD_ACCRUED,
        mapOf(
            "application_id" to applicationId,
            "application_name" to applicationName,
            "environment" to environment,
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
        applicationId: String?,
        applicationName: String?,
        environment: String?,
        currency: String,
        protocol: String,
        oldRate: Double,
        newRate: Double
    ) = sendEvent(
        accountId,
        EventTypes.RATE_CHANGED,
        mapOf(
            "application_id" to applicationId,
            "application_name" to applicationName,
            "environment" to environment,
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
        applicationId: String?,
        applicationName: String?,
        environment: String?,
        yieldAccountId: String,
        oldStatus: String,
        newStatus: String
    ) = sendEvent(
        accountId,
        EventTypes.ACCOUNT_STATUS_CHANGED,
        mapOf(
            "application_id" to applicationId,
            "application_name" to applicationName,
            "environment" to environment,
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
