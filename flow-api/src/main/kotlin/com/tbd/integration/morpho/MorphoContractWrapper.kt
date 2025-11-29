package com.tbd.integration.morpho

import com.tbd.service.Web3Service
import com.tbd.service.WalletService
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Uint128
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.*
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.TransactionManager
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Numeric
import java.io.InputStream
import java.math.BigInteger
import java.util.*

/**
 * Wrapper for Morpho Blue contract interactions
 * Uses the ABI to encode/decode function calls
 */
class MorphoContractWrapper(
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val contractAddress: String,
    private val gasProvider: ContractGasProvider
) {
    companion object {
        fun loadAbi(): String {
            val inputStream: InputStream = MorphoContractWrapper::class.java
                .classLoader
                .getResourceAsStream("abis/MorphoBlue.json")
                ?: throw IllegalStateException("MorphoBlue.json ABI not found")
            return inputStream.bufferedReader().use { it.readText() }
        }
    }
    
    /**
     * Supply assets to Morpho
     */
    fun supply(
        marketParams: MarketParams,
        assets: BigInteger,
        shares: BigInteger,
        onBehalf: String,
        data: ByteArray = ByteArray(0)
    ): EthSendTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "supply",
            listOf(
                marketParams.toAbiType(),
                Uint256(assets),
                Uint256(shares),
                Address(onBehalf),
                DynamicBytes(data)
            ),
            listOf(
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {}
            )
        )
        
        return executeTransaction(function)
    }
    
    /**
     * Withdraw assets from Morpho
     */
    fun withdraw(
        marketParams: MarketParams,
        assets: BigInteger,
        shares: BigInteger,
        onBehalf: String,
        receiver: String,
        data: ByteArray = ByteArray(0)
    ): EthSendTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "withdraw",
            listOf(
                marketParams.toAbiType(),
                Uint256(assets),
                Uint256(shares),
                Address(onBehalf),
                Address(receiver),
                DynamicBytes(data)
            ),
            listOf(
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {}
            )
        )
        
        return executeTransaction(function)
    }
    
    /**
     * Get position for a user in a market
     */
    fun getPosition(marketParams: MarketParams, user: String): Position {
        val function = org.web3j.abi.datatypes.Function(
            "position",
            listOf(
                marketParams.toAbiType(),
                Address(user)
            ),
            listOf(
                object : TypeReference<DynamicStruct>() {}
            )
        )
        
        val encodedFunction = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                credentials.address,
                contractAddress,
                encodedFunction
            ),
            DefaultBlockParameterName.LATEST
        ).send()
        
        val values = FunctionReturnDecoder.decode(
            response.value,
            function.outputParameters
        )
        
        // Parse Position struct
        val struct = values[0] as DynamicStruct
        return Position(
            supplyShares = (struct.getValue()[0] as Uint256).value,
            borrowShares = (struct.getValue()[1] as Uint128).value.toLong(),
            collateral = (struct.getValue()[2] as Uint128).value.toLong()
        )
    }
    
    private fun executeTransaction(function: org.web3j.abi.datatypes.Function): EthSendTransaction {
        val encodedFunction = FunctionEncoder.encode(function)
        val transactionManager = RawTransactionManager(web3j, credentials)
        
        return transactionManager.sendTransaction(
            gasProvider.gasPrice,
            gasProvider.gasLimit,
            contractAddress,
            encodedFunction,
            BigInteger.ZERO
        )
    }
}

data class MarketParams(
    val loanToken: String,
    val collateralToken: String,
    val oracle: String,
    val irm: String,
    val lltv: BigInteger
) {
    fun toAbiType(): DynamicStruct {
        return DynamicStruct(
            Address(loanToken),
            Address(collateralToken),
            Address(oracle),
            Address(irm),
            Uint256(lltv)
        )
    }
}

data class Position(
    val supplyShares: BigInteger,
    val borrowShares: Long,
    val collateral: Long
)

