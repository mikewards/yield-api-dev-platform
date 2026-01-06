package com.ground.dto

import kotlinx.serialization.Serializable

// ============================================
// Profile DTOs
// ============================================

@Serializable
data class UpdateEmailRequest(
    val email: String,
    val password: String  // Require password to change email
)

@Serializable
data class UpdateEmailResponse(
    val success: Boolean,
    val email: String,
    val message: String
)

// ============================================
// Password DTOs
// ============================================

@Serializable
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

@Serializable
data class ChangePasswordResponse(
    val success: Boolean,
    val message: String
)

// ============================================
// Session DTOs
// ============================================

@Serializable
data class SessionResponse(
    val session_id: String,
    val ip_address: String?,
    val device_info: String?,
    val last_active_at: String,
    val created_at: String,
    val is_current: Boolean
)

@Serializable
data class SessionsListResponse(
    val sessions: List<SessionResponse>,
    val total: Int
)

@Serializable
data class RevokeSessionResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class RevokeAllSessionsResponse(
    val success: Boolean,
    val revoked_count: Int,
    val message: String
)

// ============================================
// Account Deletion DTOs
// ============================================

@Serializable
data class DeleteAccountRequest(
    val password: String,
    val confirmation: String  // Must be "DELETE" or similar
)

@Serializable
data class DeleteAccountResponse(
    val success: Boolean,
    val message: String
)

// ============================================
// Full Account Settings Response
// ============================================

@Serializable
data class AccountSettingsResponse(
    val account_id: String,
    val username: String,
    val email: String?,
    val created_at: String,
    val status: String
)

