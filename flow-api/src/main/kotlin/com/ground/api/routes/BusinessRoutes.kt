package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

/**
 * Business management routes for RCAC.
 * 
 * Handles business CRUD, member management, and invitations.
 */
fun Application.businessRoutes() {
    val businessService = BusinessService()
    val roleService = RoleService()
    
    routing {
        authenticate("bearer-auth") {
            route("/v1/businesses") {
                
                // ═══════════════════════════════════════════════════════════
                // BUSINESS CRUD
                // ═══════════════════════════════════════════════════════════
                
                /**
                 * List all businesses the user is a member of
                 */
                get {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    
                    val businesses = businessService.listUserBusinesses(userId)
                    call.respond(businesses.map { it.toBusinessWithRolesResponse() })
                }
                
                /**
                 * Create a new business
                 */
                post {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val request = call.receive<CreateBusinessRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    try {
                        val business = businessService.createBusiness(
                            userId = userId,
                            name = request.name,
                            slug = request.slug,
                            description = request.description,
                            website = request.website,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(HttpStatusCode.Created, business.toBusinessResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse(ErrorDetail("SLUG_EXISTS", e.message ?: "Business slug already exists", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Get a specific business
                 */
                get("/{businessId}") {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@get call.respondBadRequest("Invalid business ID")
                    
                    // Check if user is a member
                    if (!businessService.isMember(userId, businessId)) {
                        return@get call.respondForbidden("Not a member of this business")
                    }
                    
                    val business = businessService.getBusiness(businessId)
                    if (business == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ErrorResponse(ErrorDetail("NOT_FOUND", "Business not found", "not_found"))
                        )
                        return@get
                    }
                    
                    call.respond(business.toBusinessResponse())
                }
                
                /**
                 * Update a business
                 */
                patch("/{businessId}") {
                    val userId = call.getCurrentUserId() ?: return@patch call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@patch call.respondBadRequest("Invalid business ID")
                    val request = call.receive<UpdateBusinessRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    try {
                        val business = businessService.updateBusiness(
                            userId = userId,
                            businessId = businessId,
                            name = request.name,
                            description = request.description,
                            website = request.website,
                            logoUrl = request.logo_url,
                            ipAddress = ipAddress
                        )
                        
                        if (business == null) {
                            return@patch call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(ErrorDetail("NOT_FOUND", "Business not found", "not_found"))
                            )
                        }
                        
                        call.respond(business.toBusinessResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respondForbidden(e.message ?: "Permission denied")
                    }
                }
                
                // ═══════════════════════════════════════════════════════════
                // MEMBER MANAGEMENT
                // ═══════════════════════════════════════════════════════════
                
                /**
                 * List members of a business
                 */
                get("/{businessId}/members") {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@get call.respondBadRequest("Invalid business ID")
                    
                    try {
                        val members = businessService.listMembers(userId, businessId)
                        call.respond(members.map { it.toMemberResponse() })
                    } catch (e: IllegalArgumentException) {
                        call.respondForbidden(e.message ?: "Permission denied")
                    }
                }
                
                /**
                 * Invite a member to the business
                 */
                post("/{businessId}/invitations") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@post call.respondBadRequest("Invalid business ID")
                    val request = call.receive<InviteMemberRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    try {
                        val roleIds = request.role_ids.mapNotNull { it.toUUIDOrNull() }
                        
                        val invitation = businessService.inviteMember(
                            inviterId = userId,
                            businessId = businessId,
                            email = request.email,
                            roleIds = roleIds,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(HttpStatusCode.Created, invitation.toInvitationResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Remove a member from the business
                 */
                delete("/{businessId}/members/{memberId}") {
                    val userId = call.getCurrentUserId() ?: return@delete call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid business ID")
                    val memberId = call.parameters["memberId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid member ID")
                    val ipAddress = call.getClientIpAddress()
                    
                    try {
                        businessService.removeMember(
                            actorId = userId,
                            businessId = businessId,
                            targetUserId = memberId,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "validation_error"))
                        )
                    }
                }
                
                // ═══════════════════════════════════════════════════════════
                // ROLE MANAGEMENT
                // ═══════════════════════════════════════════════════════════
                
                /**
                 * List roles in a business
                 */
                get("/{businessId}/roles") {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@get call.respondBadRequest("Invalid business ID")
                    
                    // Check membership
                    if (!businessService.isMember(userId, businessId)) {
                        return@get call.respondForbidden("Not a member of this business")
                    }
                    
                    val roles = roleService.listRoles(businessId)
                    call.respond(roles.map { it.toRoleResponse() })
                }
                
                /**
                 * Create a custom role
                 */
                post("/{businessId}/roles") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@post call.respondBadRequest("Invalid business ID")
                    val request = call.receive<CreateRoleRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    // Check permission
                    if (!businessService.hasBusinessPermission(userId, businessId, "roles:create")) {
                        return@post call.respondForbidden("Permission denied")
                    }
                    
                    try {
                        val role = roleService.createRole(
                            creatorId = userId,
                            businessId = businessId,
                            name = request.name,
                            displayName = request.display_name,
                            description = request.description,
                            color = request.color,
                            businessPermissions = request.business_permissions,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(HttpStatusCode.Created, role.toRoleResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.Conflict,
                            ErrorResponse(ErrorDetail("ROLE_EXISTS", e.message ?: "Role already exists", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Update a role
                 */
                patch("/{businessId}/roles/{roleId}") {
                    val userId = call.getCurrentUserId() ?: return@patch call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@patch call.respondBadRequest("Invalid business ID")
                    val roleId = call.parameters["roleId"]?.toUUIDOrNull()
                        ?: return@patch call.respondBadRequest("Invalid role ID")
                    val request = call.receive<UpdateRoleRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    // Check permission
                    if (!businessService.hasBusinessPermission(userId, businessId, "roles:edit")) {
                        return@patch call.respondForbidden("Permission denied")
                    }
                    
                    try {
                        val role = roleService.updateRole(
                            actorId = userId,
                            roleId = roleId,
                            displayName = request.display_name,
                            description = request.description,
                            color = request.color,
                            businessPermissions = request.business_permissions,
                            ipAddress = ipAddress
                        )
                        
                        if (role == null) {
                            return@patch call.respond(
                                HttpStatusCode.NotFound,
                                ErrorResponse(ErrorDetail("NOT_FOUND", "Role not found", "not_found"))
                            )
                        }
                        
                        call.respond(role.toRoleResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Cannot modify system role", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Delete a role
                 */
                delete("/{businessId}/roles/{roleId}") {
                    val userId = call.getCurrentUserId() ?: return@delete call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid business ID")
                    val roleId = call.parameters["roleId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid role ID")
                    val ipAddress = call.getClientIpAddress()
                    
                    // Check permission
                    if (!businessService.hasBusinessPermission(userId, businessId, "roles:delete")) {
                        return@delete call.respondForbidden("Permission denied")
                    }
                    
                    try {
                        roleService.deleteRole(userId, roleId, ipAddress)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Cannot delete system role", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Assign a role to a user
                 */
                post("/{businessId}/roles/{roleId}/assign") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@post call.respondBadRequest("Invalid business ID")
                    val roleId = call.parameters["roleId"]?.toUUIDOrNull()
                        ?: return@post call.respondBadRequest("Invalid role ID")
                    val request = call.receive<AssignRoleRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    // Check permission
                    if (!businessService.hasBusinessPermission(userId, businessId, "members:manage_roles")) {
                        return@post call.respondForbidden("Permission denied")
                    }
                    
                    try {
                        val targetUserId = request.user_id.toUUIDOrNull()
                            ?: return@post call.respondBadRequest("Invalid user ID")
                        val expiresAt = request.expires_at?.let { java.time.Instant.parse(it) }
                        
                        roleService.assignRole(
                            actorId = userId,
                            userId = targetUserId,
                            roleId = roleId,
                            expiresAt = expiresAt,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(HttpStatusCode.Created, mapOf("message" to "Role assigned"))
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Invalid request", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Revoke a role from a user
                 */
                delete("/{businessId}/roles/{roleId}/users/{targetUserId}") {
                    val userId = call.getCurrentUserId() ?: return@delete call.respondUnauthorized()
                    val businessId = call.parameters["businessId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid business ID")
                    val roleId = call.parameters["roleId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid role ID")
                    val targetUserId = call.parameters["targetUserId"]?.toUUIDOrNull()
                        ?: return@delete call.respondBadRequest("Invalid target user ID")
                    val ipAddress = call.getClientIpAddress()
                    
                    // Check permission
                    if (!businessService.hasBusinessPermission(userId, businessId, "members:manage_roles")) {
                        return@delete call.respondForbidden("Permission denied")
                    }
                    
                    try {
                        roleService.revokeRole(userId, targetUserId, roleId, ipAddress)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Cannot revoke owner role", "validation_error"))
                        )
                    }
                }
            }
            
            // ═══════════════════════════════════════════════════════════
            // INVITATION ROUTES (user-facing)
            // ═══════════════════════════════════════════════════════════
            
            route("/v1/invitations") {
                
                /**
                 * Accept an invitation
                 */
                post("/accept") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val request = call.receive<AcceptInvitationRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    try {
                        val business = businessService.acceptInvitation(
                            userId = userId,
                            token = request.token,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(business.toBusinessWithRolesResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_INVITATION", e.message ?: "Invalid or expired invitation", "validation_error"))
                        )
                    }
                }
                
                /**
                 * Decline an invitation
                 */
                post("/decline") {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val request = call.receive<AcceptInvitationRequest>()
                    val ipAddress = call.getClientIpAddress()
                    
                    try {
                        businessService.declineInvitation(
                            userId = userId,
                            token = request.token,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(mapOf("message" to "Invitation declined"))
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_INVITATION", e.message ?: "Invalid invitation", "validation_error"))
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS FOR RESPONSE MAPPING
// ═══════════════════════════════════════════════════════════════

private fun com.ground.service.BusinessResponse.toBusinessResponse() = BusinessResponse(
    id = this.id.toString(),
    name = this.name,
    slug = this.slug,
    logo_url = this.logoUrl,
    description = this.description,
    website = this.website,
    plan = this.plan,
    status = this.status,
    owner_id = this.ownerId.toString(),
    created_at = this.createdAt?.toString() ?: "",
    updated_at = this.updatedAt?.toString()
)

private fun com.ground.service.BusinessWithRoles.toBusinessWithRolesResponse() = BusinessWithRolesResponse(
    id = this.id.toString(),
    name = this.name,
    slug = this.slug,
    logo_url = this.logoUrl,
    description = this.description,
    website = this.website,
    plan = this.plan,
    status = this.status,
    is_owner = this.isOwner,
    roles = this.roles.map { it.toRoleInfoResponse() },
    created_at = this.createdAt?.toString() ?: "",
    updated_at = this.updatedAt?.toString()
)

private fun com.ground.service.MemberInfo.toMemberResponse() = MemberResponse(
    user_id = this.userId.toString(),
    email = this.email,
    first_name = this.firstName,
    last_name = this.lastName,
    avatar_url = this.avatarUrl,
    roles = this.roles.map { it.toRoleInfoResponse() },
    is_owner = this.isOwner,
    joined_at = this.joinedAt?.toString()
)

private fun com.ground.service.InvitationResponse.toInvitationResponse() = com.ground.dto.InvitationResponse(
    membership_id = this.membershipId.toString(),
    business_id = this.businessId.toString(),
    business_name = this.businessName,
    email = this.email,
    token = this.token,
    expires_at = this.expiresAt.toString(),
    role_ids = this.roleIds.map { it.toString() }
)

private fun com.ground.service.RoleInfo.toRoleInfoResponse() = RoleInfoResponse(
    id = this.id.toString(),
    name = this.name,
    display_name = this.displayName,
    color = this.color,
    is_system = this.isSystem
)

private fun com.ground.service.RoleResponse.toRoleResponse() = com.ground.dto.RoleResponse(
    id = this.id.toString(),
    business_id = this.businessId.toString(),
    name = this.name,
    display_name = this.displayName,
    description = this.description,
    color = this.color,
    is_system = this.isSystem,
    is_default = this.isDefault,
    business_permissions = this.businessPermissions,
    member_count = this.memberCount,
    created_at = this.createdAt?.toString() ?: "",
    updated_at = this.updatedAt?.toString()
)

// ═══════════════════════════════════════════════════════════════
// UTILITY EXTENSIONS
// ═══════════════════════════════════════════════════════════════

private fun String.toUUIDOrNull(): UUID? = try { UUID.fromString(this) } catch (e: Exception) { null }
