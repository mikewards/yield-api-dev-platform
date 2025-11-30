package com.tbd.dto

import kotlinx.serialization.Serializable

@Serializable
data class Market(
    val market_id: String,
    val protocol: String,
    val currency: String,
    val currency_address: String? = null,
    val network: String,
    val apy: Double,
    val status: String = "active",
    val updated_at: String
)

@Serializable
data class MarketsResponse(
    val markets: List<Market>
)

