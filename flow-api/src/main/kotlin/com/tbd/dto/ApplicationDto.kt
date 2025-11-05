package com.tbd.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateApplicationRequest(
    val name: String,
    val description: String? = null,
    val environment: String? = null, // Optional - defaults to "both" in backend
    val webhook_url: String? = null,
    val allowed_origins: List<String>? = null,
    val permissions: List<String>? = null,
    val sandbox_rpc_url: String? = null,
    val production_rpc_url: String? = null
)

@Serializable
data class UpdateApplicationRequest(
    val name: String? = null,
    val description: String? = null,
    val webhook_url: String? = null,
    val allowed_origins: List<String>? = null,
    val permissions: List<String>? = null,
    val status: String? = null,
    val sandbox_rpc_url: String? = null,
    val production_rpc_url: String? = null
)

@Serializable
data class ApplicationResponse(
    val application_id: String,
    val name: String,
    val description: String?,
    val environment: String,
    val status: String,
    val webhook_url: String?,
    val webhook_secret: String?,
    val allowed_origins: List<String>?,
    val permissions: List<String>?,
    val sandbox_rpc_url: String?,
    val production_rpc_url: String?,
    val created_at: String,
    val updated_at: String?
)

@Serializable
data class ApplicationListResponse(
    val applications: List<ApplicationResponse>
)

@Serializable
data class ApplicationCredentials(
    val application_id: String,
    val client_id: String,
    val client_secret: String,
    val environment: String
)

// Updated Token DTOs to include application scope
@Serializable
data class CreateAppTokenRequest(
    val name: String,
    val environment: String? = null, // sandbox or production - required when creating token
    val expires_in: Int? = null,
    val permissions: List<String>? = null
)

@Serializable
data class AppTokenResponse(
    val token_id: String,
    val application_id: String,
    val access_token: String,
    val name: String,
    val environment: String,
    val permissions: List<String>?,
    val created_at: String,
    val expires_at: String?,
    val last_used_at: String?
)

@Serializable
data class AppTokenListResponse(
    val tokens: List<AppTokenResponse>
)

// Wallet DTOs
@Serializable
data class CreateWalletRequest(
    val label: String? = null,
    val chain: String = "ethereum"
)

@Serializable
data class WalletResponse(
    val wallet_id: String,
    val address: String,
    val environment: String,
    val chain: String,
    val label: String?,
    val status: String,
    val created_at: String
)

@Serializable
data class WalletListResponse(
    val wallets: List<WalletResponse>
)

