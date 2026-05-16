package com.ground.dto

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════
// BUSINESS DTOs
// ═══════════════════════════════════════════════════════════════

@Serializable
data class CreateBusinessRequest(
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    val website: String? = null
)

@Serializable
data class UpdateBusinessRequest(
    val name: String? = null,
    val description: String? = null,
    val website: String? = null,
    val logo_url: String? = null
)

@Serializable
data class BusinessResponse(
    val id: String,
    val name: String,
    val slug: String,
    val logo_url: String? = null,
    val description: String? = null,
    val website: String? = null,
    val plan: String,
    val status: String,
    val owner_id: String,
    val created_at: String,
    val updated_at: String? = null
)

@Serializable
data class BusinessWithRolesResponse(
    val id: String,
    val name: String,
    val slug: String,
    val logo_url: String? = null,
    val description: String? = null,
    val website: String? = null,
    val plan: String,
    val status: String,
    val is_owner: Boolean,
    val roles: List<RoleInfoResponse>,
    val created_at: String,
    val updated_at: String? = null
)

// ═══════════════════════════════════════════════════════════════
// MEMBERSHIP DTOs
// ═══════════════════════════════════════════════════════════════

@Serializable
data class InviteMemberRequest(
    val email: String,
    val role_ids: List<String>
)

@Serializable
data class InvitationResponse(
    val membership_id: String,
    val business_id: String,
    val business_name: String,
    val email: String,
    val token: String,
    val expires_at: String,
    val role_ids: List<String>
)

@Serializable
data class AcceptInvitationRequest(
    val token: String
)

@Serializable
data class MemberResponse(
    val user_id: String,
    val email: String,
    val first_name: String? = null,
    val last_name: String? = null,
    val avatar_url: String? = null,
    val roles: List<RoleInfoResponse>,
    val is_owner: Boolean,
    val joined_at: String? = null
)

@Serializable
data class UpdateMemberRolesRequest(
    val role_ids: List<String>
)

// ═══════════════════════════════════════════════════════════════
// ROLE DTOs
// ═══════════════════════════════════════════════════════════════

@Serializable
data class CreateRoleRequest(
    val name: String,
    val display_name: String,
    val description: String? = null,
    val color: String? = null,
    val business_permissions: List<String> = emptyList()
)

@Serializable
data class UpdateRoleRequest(
    val display_name: String? = null,
    val description: String? = null,
    val color: String? = null,
    val business_permissions: List<String>? = null
)

@Serializable
data class RoleResponse(
    val id: String,
    val business_id: String,
    val name: String,
    val display_name: String,
    val description: String? = null,
    val color: String? = null,
    val is_system: Boolean,
    val is_default: Boolean,
    val business_permissions: List<String>,
    val member_count: Int? = null,
    val created_at: String,
    val updated_at: String? = null
)

@Serializable
data class RoleInfoResponse(
    val id: String,
    val name: String,
    val display_name: String,
    val color: String? = null,
    val is_system: Boolean
)

@Serializable
data class AssignRoleRequest(
    val user_id: String,
    val expires_at: String? = null
)

// ═══════════════════════════════════════════════════════════════
// RESOURCE ACCESS DTOs
// ═══════════════════════════════════════════════════════════════

@Serializable
data class GrantAccessRequest(
    val user_id: String? = null,
    val role_id: String? = null,
    val permission: String, // "read", "write", "admin"
    val reason: String? = null,
    val expires_at: String? = null
)

@Serializable
data class ResourceAccessResponse(
    val id: String,
    val resource_type: String,
    val resource_id: String,
    val user_id: String? = null,
    val user_email: String? = null,
    val role_id: String? = null,
    val role_name: String? = null,
    val role_display_name: String? = null,
    val permission: String,
    val granted_by: String,
    val granted_at: String,
    val expires_at: String? = null
)

@Serializable
data class MyPermissionResponse(
    val permission: String?, // null if no access
    val access_source: String? = null // "direct" or "role"
)

