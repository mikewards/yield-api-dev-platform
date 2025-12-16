package com.tbd.service

import org.slf4j.LoggerFactory

/**
 * Alchemy RPC URL provisioning service.
 * 
 * Uses a shared Alchemy API key to construct RPC URLs for users.
 * This approach is simpler and more reliable than creating individual apps via API.
 */
data class AlchemyProvisionedKeys(
    val sandboxRpcUrl: String,
    val sandboxApiKey: String,
    val productionRpcUrl: String,
    val productionApiKey: String
)

class AlchemyService {
    private val logger = LoggerFactory.getLogger(AlchemyService::class.java)
    
    // Alchemy App API Key (not the Access Token)
    private val alchemyApiKey: String? = System.getenv("ALCHEMY_API_KEY")
    
    companion object {
        // Alchemy RPC URL templates
        private const val ALCHEMY_SEPOLIA_TEMPLATE = "https://eth-sepolia.g.alchemy.com/v2/"
        private const val ALCHEMY_MAINNET_TEMPLATE = "https://eth-mainnet.g.alchemy.com/v2/"
        
        // Fallback to public RPCs if Alchemy not configured
        const val FALLBACK_SANDBOX_RPC = "https://rpc.sepolia.org"
        const val FALLBACK_PRODUCTION_RPC = "https://eth.llamarpc.com"
    }
    
    /**
     * Check if Alchemy integration is configured
     */
    fun isConfigured(): Boolean {
        val configured = !alchemyApiKey.isNullOrBlank()
        logger.info("Alchemy configured: $configured, key present: ${alchemyApiKey?.take(8)}...")
        return configured
    }
    
    /**
     * Provision Alchemy RPC URLs using the shared API key.
     * All users share the same Alchemy infrastructure (common pattern for platforms).
     */
    fun provisionApps(appName: String): AlchemyProvisionedKeys {
        logger.info("Provisioning RPC URLs for app: $appName")
        
        if (!isConfigured()) {
            logger.warn("ALCHEMY_API_KEY not configured, using fallback public RPCs")
            return AlchemyProvisionedKeys(
                sandboxRpcUrl = FALLBACK_SANDBOX_RPC,
                sandboxApiKey = "",
                productionRpcUrl = FALLBACK_PRODUCTION_RPC,
                productionApiKey = ""
            )
        }
        
        val sandboxUrl = "$ALCHEMY_SEPOLIA_TEMPLATE$alchemyApiKey"
        val productionUrl = "$ALCHEMY_MAINNET_TEMPLATE$alchemyApiKey"
        
        logger.info("Provisioned Alchemy URLs - Sandbox: ${sandboxUrl.take(50)}..., Production: ${productionUrl.take(50)}...")
        
        return AlchemyProvisionedKeys(
            sandboxRpcUrl = sandboxUrl,
            sandboxApiKey = alchemyApiKey!!,
            productionRpcUrl = productionUrl,
            productionApiKey = alchemyApiKey
        )
    }
}

