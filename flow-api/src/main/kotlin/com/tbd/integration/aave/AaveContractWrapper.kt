package com.tbd.integration.aave

import com.tbd.service.Web3Service
import com.tbd.service.WalletService
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Uint16
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.*
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.ContractGasProvider
import java.io.InputStream
import java.math.BigInteger
import java.util.*

/**
 * Wrapper for Aave Pool contract interactions
 * Uses the ABI to encode/decode function calls
 */
class AaveContractWrapper(
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val contractAddress: String,
    private val gasProvider: ContractGasProvider
) {
    companion object {
        fun loadAbi(): String {
            val inputStream: InputStream = AaveContractWrapper::class.java
                .classLoader
                .getResourceAsStream("abis/AavePool.json")
                ?: throw IllegalStateException("AavePool.json ABI not found")
            return inputStream.bufferedReader().use { it.readText() }
        }
    }
    
    /**
     * Supply assets to Aave
     */
    fun supply(
        asset: String,
        amount: BigInteger,
        onBehalfOf: String,
        referralCode: Int = 0
    ): EthSendTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "supply",
            listOf(
                Address(asset),
                Uint256(amount),
                Address(onBehalfOf),
                Uint16(BigInteger.valueOf(referralCode.toLong()))
            ),
            emptyList()
        )
        
        return executeTransaction(function)
    }
    
    /**
     * Withdraw assets from Aave
     */
    fun withdraw(
        asset: String,
        amount: BigInteger,
        to: String
    ): EthSendTransaction {
        val function = org.web3j.abi.datatypes.Function(
            "withdraw",
            listOf(
                Address(asset),
                Uint256(amount),
                Address(to)
            ),
            listOf(
                object : TypeReference<Uint256>() {}
            )
        )
        
        return executeTransaction(function)
    }
    
    /**
     * Get user account data
     */
    fun getUserAccountData(user: String): UserAccountData {
        val function = org.web3j.abi.datatypes.Function(
            "getUserAccountData",
            listOf(Address(user)),
            listOf(
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {},
                object : TypeReference<Uint256>() {}
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
        
        return UserAccountData(
            totalCollateralBase = (values[0] as Uint256).value,
            totalDebtBase = (values[1] as Uint256).value,
            availableBorrowsBase = (values[2] as Uint256).value,
            currentLiquidationThreshold = (values[3] as Uint256).value,
            ltv = (values[4] as Uint256).value,
            healthFactor = (values[5] as Uint256).value
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

data class UserAccountData(
    val totalCollateralBase: BigInteger,
    val totalDebtBase: BigInteger,
    val availableBorrowsBase: BigInteger,
    val currentLiquidationThreshold: BigInteger,
    val ltv: BigInteger,
    val healthFactor: BigInteger
)

