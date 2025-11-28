package com.flow.dto

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class CreateAccountRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

@Serializable
data class AccountResponse(
    val account_id: String,
    val username: String,
    val created_at: String,
    val status: String
)

@Serializable
data class AuthenticateRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthenticateResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val account_id: String
)
