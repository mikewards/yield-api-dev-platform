package com.tbd.service

import com.tbd.integration.morpho.MarketParams
import com.typesafe.config.ConfigFactory
import java.math.BigInteger

/**
 * Service for looking up Morpho market parameters
 * Morpho uses market IDs based on loan token, collateral token, oracle, IRM, and LLTV
 */
class MorphoMarketService {
    private val config = ConfigFactory.load()
    
    // Common token addresses (Ethereum mainnet)
    private val USDC = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
    private val USDT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    private val DAI = "0x6B175474E89094C44Da98b954EedeAC495271d0F"
    private val WETH = "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
    private val WBTC = "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599"
    
    // Common oracles (Chainlink)
    private val CHAINLINK_USDC_USD = "0x8fFfFfd4AfB6115b954Bd326cbe7B4BA576818f6"
    private val CHAINLINK_USDT_USD = "0x3E7d1eAB13ad0104d2750B8463F1C9C19C59e6E4"
    private val CHAINLINK_DAI_USD = "0xAed0c38402a5d19df6E4c03F4E2DceD6e29c1ee9"
    private val CHAINLINK_ETH_USD = "0x5f4eC3Df9cbd43714FE2740f5E3616155c5b8419"
    private val CHAINLINK_BTC_USD = "0xF4030086522a5bEEa4988F8cA5B36dbC97BeE88c"
    
    // Common IRMs (Interest Rate Models) - using default Morpho IRM
    private val DEFAULT_IRM = "0x870aC11D48B0DB3459D7A535a4dC6c1b5E2935e5" // Example - update with actual
    
    /**
     * Get market parameters for a common currency pair
     */
    fun getMarketParams(currency: String, environment: String): MarketParams {
        val loanToken = getTokenAddress(currency)
        val collateralToken = loanToken // Same token for supply markets
        val oracle = getOracleAddress(currency)
        val irm = DEFAULT_IRM
        val lltv = getDefaultLLTV(currency) // Loan-to-value threshold (typically 95% = 0.95 * 1e18)
        
        return MarketParams(
            loanToken = loanToken,
            collateralToken = collateralToken,
            oracle = oracle,
            irm = irm,
            lltv = lltv
        )
    }
    
    /**
     * Get market parameters for a custom pair
     */
    fun getMarketParams(
        loanToken: String,
        collateralToken: String,
        oracle: String,
        irm: String,
        lltv: BigInteger
    ): MarketParams {
        return MarketParams(
            loanToken = loanToken,
            collateralToken = collateralToken,
            oracle = oracle,
            irm = irm,
            lltv = lltv
        )
    }
    
    private fun getTokenAddress(currency: String): String {
        return when (currency.uppercase()) {
            "USDC" -> USDC
            "USDT" -> USDT
            "DAI" -> DAI
            "ETH", "WETH" -> WETH
            "WBTC", "BTC" -> WBTC
            else -> throw IllegalArgumentException("Unsupported currency: $currency")
        }
    }
    
    private fun getOracleAddress(currency: String): String {
        return when (currency.uppercase()) {
            "USDC" -> CHAINLINK_USDC_USD
            "USDT" -> CHAINLINK_USDT_USD
            "DAI" -> CHAINLINK_DAI_USD
            "ETH", "WETH" -> CHAINLINK_ETH_USD
            "WBTC", "BTC" -> CHAINLINK_BTC_USD
            else -> throw IllegalArgumentException("Unsupported currency: $currency")
        }
    }
    
    private fun getDefaultLLTV(currency: String): BigInteger {
        // Default LLTV is typically 95% (0.95 * 1e18)
        // This means you can borrow up to 95% of your collateral value
        return BigInteger("950000000000000000") // 0.95 * 10^18
    }
    
    /**
     * List available markets
     */
    fun listAvailableMarkets(): List<String> {
        return listOf("USDC", "USDT", "DAI", "ETH", "WBTC")
    }
}

