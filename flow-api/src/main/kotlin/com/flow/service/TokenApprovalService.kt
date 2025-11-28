package com.flow.service

import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.*
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger
import java.util.*

/**
 * Service for handling ERC20 token approvals
 * Required before calling supply() on Morpho/Aave
 */
class TokenApprovalService(
    private val web3j: Web3j,
    private val credentials: Credentials,
    private val gasProvider: ContractGasProvider
) {
    
    companion object {
        // ERC20 approve function signature
        private const val APPROVE_FUNCTION = "approve"
        // ERC20 allowance function signature
        private const val ALLOWANCE_FUNCTION = "allowance"
    }
    
    /**
     * Check current allowance for a token
     */
    fun checkAllowance(tokenAddress: String, spenderAddress: String): BigInteger {
        val function = org.web3j.abi.datatypes.Function(
            ALLOWANCE_FUNCTION,
            listOf(
                Address(credentials.address),
                Address(spenderAddress)
            ),
            listOf(object : TypeReference<Uint256>() {})
        )
        
        val encodedFunction = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                credentials.address,
                tokenAddress,
                encodedFunction
            ),
            DefaultBlockParameterName.LATEST
        ).send()
        
        val values = FunctionReturnDecoder.decode(response.value, function.outputParameters)
        return (values[0] as Uint256).value
    }
    
    /**
     * Approve a spender to spend tokens
     */
    fun approve(tokenAddress: String, spenderAddress: String, amount: BigInteger): EthSendTransaction {
        val function = org.web3j.abi.datatypes.Function(
            APPROVE_FUNCTION,
            listOf(
                Address(spenderAddress),
                Uint256(amount)
            ),
            emptyList()
        )
        
        val encodedFunction = FunctionEncoder.encode(function)
        val transactionManager = RawTransactionManager(web3j, credentials)
        
        return transactionManager.sendTransaction(
            gasProvider.gasPrice,
            gasProvider.gasLimit,
            tokenAddress,
            encodedFunction,
            BigInteger.ZERO
        )
    }
    
    /**
     * Ensure sufficient allowance exists, approve if needed
     * Returns true if approval was needed and executed, false if already sufficient
     */
    fun ensureAllowance(
        tokenAddress: String,
        spenderAddress: String,
        requiredAmount: BigInteger
    ): Boolean {
        val currentAllowance = checkAllowance(tokenAddress, spenderAddress)
        
        // If allowance is sufficient, no action needed
        if (currentAllowance >= requiredAmount) {
            return false
        }
        
        // Approve maximum amount (2^256 - 1) for efficiency
        // This avoids needing to approve again for future transactions
        val maxApproval = BigInteger("115792089237316195423570985008687907853269984665640564039457")
        approve(tokenAddress, spenderAddress, maxApproval)
        
        return true
    }
    
    /**
     * Wait for transaction receipt
     */
    fun waitForReceipt(transactionHash: String): TransactionReceipt {
        var receipt: TransactionReceipt? = null
        var attempts = 0
        val maxAttempts = 50
        
        while (receipt == null && attempts < maxAttempts) {
            val response = web3j.ethGetTransactionReceipt(transactionHash).send()
            receipt = response.transactionReceipt.orElse(null)
            
            if (receipt == null) {
                Thread.sleep(2000) // Wait 2 seconds
                attempts++
            }
        }
        
        return receipt ?: throw IllegalStateException("Transaction receipt not found after $maxAttempts attempts")
    }
}

