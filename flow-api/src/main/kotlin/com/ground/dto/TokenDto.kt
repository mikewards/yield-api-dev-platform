package com.ground.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTokenRequest(
    val name: String,
    val expires_in: Int? = null
)

@Serializable
data class TokenResponse(
    val token_id: String,
    val access_token: String,
    val name: String,
    val created_at: String,
    val expires_at: String? = null
)

@Serializable
data class TokenListResponse(
    val tokens: List<TokenResponse>
)
