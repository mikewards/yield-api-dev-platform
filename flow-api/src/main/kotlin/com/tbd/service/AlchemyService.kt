package com.tbd.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class AlchemyCreateAppRequest(
    val name: String,
    val network: String,  // "ETH_SEPOLIA" or "ETH_MAINNET"
    val description: String? = null
)

@Serializable
data class AlchemyAppResponse(
    val id: String? = null,
    val name: String? = null,
    val network: String? = null,
    val apiKey: String? = null,
    val httpsUrl: String? = null,
    val wssUrl: String? = null,
    val error: String? = null
)

@Serializable
data class AlchemyProvisionedKeys(
    val sandboxRpcUrl: String,
    val sandboxApiKey: String,
    val productionRpcUrl: String,
    val productionApiKey: String
)

class AlchemyService {
    private val logger = LoggerFactory.getLogger(AlchemyService::class.java)
    
    private val adminApiKey: String? = System.getenv("ALCHEMY_ADMIN_API_KEY")
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    companion object {
        private const val ALCHEMY_API_BASE = "https://dashboard.alchemy.com/api"
        
        // Fallback to public RPCs if Alchemy fails
        const val FALLBACK_SANDBOX_RPC = "https://rpc.sepolia.org"
        const val FALLBACK_PRODUCTION_RPC = "https://eth.llamarpc.com"
    }
    
    /**
     * Check if Alchemy integration is configured
     */
    fun isConfigured(): Boolean {
        return !adminApiKey.isNullOrBlank()
    }
    
    /**
     * Provision Alchemy apps for both sandbox and production environments
     * Returns RPC URLs with embedded API keys
     */
    suspend fun provisionApps(appName: String): AlchemyProvisionedKeys {
        if (!isConfigured()) {
            logger.warn("Alchemy admin API key not configured, using fallback public RPCs")
            return AlchemyProvisionedKeys(
                sandboxRpcUrl = FALLBACK_SANDBOX_RPC,
                sandboxApiKey = "",
                productionRpcUrl = FALLBACK_PRODUCTION_RPC,
                productionApiKey = ""
            )
        }
        
        try {
            // Create sandbox app (Sepolia testnet)
            val sandboxApp = createApp("$appName-sandbox", "ETH_SEPOLIA")
            
            // Create production app (Ethereum mainnet)
            val productionApp = createApp("$appName-production", "ETH_MAINNET")
            
            return AlchemyProvisionedKeys(
                sandboxRpcUrl = sandboxApp.httpsUrl ?: FALLBACK_SANDBOX_RPC,
                sandboxApiKey = sandboxApp.apiKey ?: "",
                productionRpcUrl = productionApp.httpsUrl ?: FALLBACK_PRODUCTION_RPC,
                productionApiKey = productionApp.apiKey ?: ""
            )
        } catch (e: Exception) {
            logger.error("Failed to provision Alchemy apps: ${e.message}", e)
            return AlchemyProvisionedKeys(
                sandboxRpcUrl = FALLBACK_SANDBOX_RPC,
                sandboxApiKey = "",
                productionRpcUrl = FALLBACK_PRODUCTION_RPC,
                productionApiKey = ""
            )
        }
    }
    
    /**
     * Create a single Alchemy app
     */
    private suspend fun createApp(name: String, network: String): AlchemyAppResponse {
        logger.info("Creating Alchemy app: $name on network: $network")
        
        try {
            val response = client.post("$ALCHEMY_API_BASE/create-app") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $adminApiKey")
                setBody(AlchemyCreateAppRequest(
                    name = name,
                    network = network,
                    description = "Auto-provisioned by TBD Platform"
                ))
            }
            
            if (response.status.isSuccess()) {
                val app = response.body<AlchemyAppResponse>()
                logger.info("Successfully created Alchemy app: ${app.name} with key: ${app.apiKey?.take(10)}...")
                return app
            } else {
                val errorBody = response.body<AlchemyAppResponse>()
                logger.error("Alchemy API error: ${response.status} - ${errorBody.error}")
                throw Exception("Alchemy API error: ${errorBody.error}")
            }
        } catch (e: Exception) {
            logger.error("Failed to create Alchemy app: ${e.message}")
            throw e
        }
    }
    
    /**
     * Delete an Alchemy app (for cleanup)
     */
    suspend fun deleteApp(appId: String): Boolean {
        if (!isConfigured()) return false
        
        return try {
            val response = client.delete("$ALCHEMY_API_BASE/apps/$appId") {
                header("Authorization", "Bearer $adminApiKey")
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.error("Failed to delete Alchemy app: ${e.message}")
            false
        }
    }
}

