package com.flow.service

import com.typesafe.config.ConfigFactory
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

class Web3Service {
    private val config = ConfigFactory.load()
    
    fun getWeb3j(environment: String, applicationRpcUrl: String? = null): Web3j {
        val rpcUrl = applicationRpcUrl ?: when (environment) {
            "production" -> System.getenv("ETH_PRODUCTION_RPC_URL") 
                ?: config.getString("ethereum.production.rpcUrl")
            else -> System.getenv("ETH_SANDBOX_RPC_URL") 
                ?: config.getString("ethereum.sandbox.rpcUrl")
        }
        
        return Web3j.build(HttpService(rpcUrl))
    }
    
    fun getChainId(environment: String): BigInteger {
        return when (environment) {
            "production" -> BigInteger.valueOf(config.getLong("ethereum.production.chainId"))
            else -> BigInteger.valueOf(config.getLong("ethereum.sandbox.chainId"))
        }
    }
    
    fun getMorphoAddress(environment: String): String {
        return when (environment) {
            "production" -> config.getString("ethereum.production.morphoAddress")
            else -> config.getString("ethereum.sandbox.morphoAddress")
        }
    }
    
    fun getAaveAddress(environment: String): String {
        return when (environment) {
            "production" -> config.getString("ethereum.production.aaveAddress")
            else -> config.getString("ethereum.sandbox.aaveAddress")
        }
    }
    
    fun getGasProvider() = DefaultGasProvider()
}

