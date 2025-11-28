package com.flow.integration

import com.flow.integration.morpho.MorphoClient
import com.flow.integration.aave.AaveClient
import kotlinx.coroutines.runBlocking

class ProtocolService {
    private val morphoClient = MorphoClient()
    private val aaveClient = AaveClient()
    
    suspend fun selectBestProtocol(currency: String): String {
        // Compare rates from both protocols and select the best
        val morphoRate = morphoClient.getCurrentRate(currency)
        val aaveRate = aaveClient.getCurrentRate(currency)
        
        return if (morphoRate >= aaveRate) "morpho" else "aave"
    }
    
    suspend fun getMorphoRates(currency: String): Double {
        return morphoClient.getCurrentRate(currency)
    }
    
    suspend fun getAaveRates(currency: String): Double {
        return aaveClient.getCurrentRate(currency)
    }
    
    // Synchronous wrapper for backwards compatibility
    fun selectBestProtocolSync(currency: String): String {
        return runBlocking { selectBestProtocol(currency) }
    }
}
