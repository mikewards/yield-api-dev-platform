package com.tbd.dto

import kotlinx.serialization.Serializable

@Serializable
data class Money(
    val amount: String,
    val currency: String
)

@Serializable
data class CreateYieldAccountRequest(
    val currency: String,
    val initial_deposit: Money? = null,
    val protocol_preference: String = "auto"
)

@Serializable
data class YieldAccountResponse(
    val account_id: String,
    val currency: String,
    val protocol: String,
    val annual_yield_rate: Double,
    val status: String,
    val created_at: String,
    val balance: Money
)

@Serializable
data class DepositRequest(
    val amount: String,
    val currency: String,
    val source_address: String? = null
)

@Serializable
data class WithdrawRequest(
    val amount: String,
    val currency: String,
    val destination_address: String
)

@Serializable
data class TransactionResponse(
    val transaction_id: String,
    val account_id: String,
    val amount: Money,
    val status: String,
    val created_at: String,
    val destination_address: String? = null
)

@Serializable
data class YieldRate(
    val currency: String,
    val protocol: String,
    val annual_yield_rate: Double,
    val apy: Double,
    val updated_at: String,
    val note: String? = null
)

@Serializable
data class YieldRatesResponse(
    val rates: List<YieldRate>
)
