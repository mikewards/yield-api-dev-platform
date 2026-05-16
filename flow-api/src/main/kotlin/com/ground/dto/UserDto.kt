package com.ground.dto

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════
// USER DTOs
// ═══════════════════════════════════════════════════════════════

@Serializable
data class RegisterUserRequest(
    val email: String,
    val password: String,
    val first_name: String? = null,
    val last_name: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val mfa_code: String? = null
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "Bearer",
    val expires_in: Int,
    val user: UserResponse,
    val mfa_required: Boolean = false
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val first_name: String? = null,
    val last_name: String? = null,
    val avatar_url: String? = null,
    val status: String,
    val email_verified: Boolean,
    val mfa_enabled: Boolean,
    val last_login_at: String? = null,
    val created_at: String,
    val updated_at: String? = null
)

@Serializable
data class UpdateUserRequest(
    val first_name: String? = null,
    val last_name: String? = null,
    val avatar_url: String? = null
)

// Note: ChangePasswordRequest is defined in AccountSettingsDto.kt
// Note: DeleteAccountRequest is defined in AccountSettingsDto.kt (with confirmation field)

@Serializable
data class EnableMfaRequest(
    val secret: String,
    val code: String // Verification code to confirm MFA setup
)

@Serializable
data class MfaSetupResponse(
    val secret: String,
    val qr_code_url: String
)

