package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.*
import com.ground.model.Users
import com.ground.model.Roles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * Resource access management routes for RCAC.
 * 
 * These routes handle granting, revoking, and querying resource access.
 */
fun Application.resourceAccessRoutes() {
    val permissionService = PermissionService()
    
    routing {
        authenticate("bearer-auth") {
            route("/v1/resources/{resourceType}/{resourceId}/access") {
                
                /**
                 * List all access grants for a resource
                 * Requires admin permission on the resource
                 */
                get {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    val resourceType = call.parameters["resourceType"]
                        ?: return@get call.respondBadRequest("Resource type required")
                    val resourceId = call.parameters["resourceId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    } ?: return@get call.respondBadRequest("Invalid resource ID")
                    
                    try {
                        val accessList = permissionService.getResourceAccessList(userId, resourceType, resourceId)
                        call.respond(accessList.map { it.toResponse() })
                    } catch (e: IllegalArgumentException) {
                        call.respondForbidden(e.message ?: "Admin permission required")
                    }
                }
                
                /**
                 * Grant access to a resource
                 * Requires admin permission on the resource
                 */
                post {
                    val userId = call.getCurrentUserId() ?: return@post call.respondUnauthorized()
                    val resourceType = call.parameters["resourceType"]
                        ?: return@post call.respondBadRequest("Resource type required")
                    val resourceId = call.parameters["resourceId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    } ?: return@post call.respondBadRequest("Invalid resource ID")
                    val request = call.receive<GrantAccessRequest>()
                    val ipAddress = getClientIp(call)
                    
                    // Validate that either user_id or role_id is provided, not both
                    if ((request.user_id == null && request.role_id == null) ||
                        (request.user_id != null && request.role_id != null)) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", "Provide either user_id or role_id, not both", "validation_error"))
                        )
                    }
                    
                    // Validate permission level
                    if (request.permission !in listOf("read", "write", "admin")) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_PERMISSION", "Permission must be read, write, or admin", "validation_error"))
                        )
                    }
                    
                    try {
                        val grant = if (request.user_id != null) {
                            permissionService.grantUserAccess(
                                grantorId = userId,
                                targetUserId = UUID.fromString(request.user_id),
                                resourceType = resourceType,
                                resourceId = resourceId,
                                permission = request.permission,
                                reason = request.reason,
                                expiresAt = request.expires_at?.let { Instant.parse(it) },
                                ipAddress = ipAddress
                            )
                        } else {
                            permissionService.grantRoleAccess(
                                grantorId = userId,
                                roleId = UUID.fromString(request.role_id!!),
                                resourceType = resourceType,
                                resourceId = resourceId,
                                permission = request.permission,
                                reason = request.reason,
                                ipAddress = ipAddress
                            )
                        }
                        
                        call.respond(HttpStatusCode.Created, grant.toResponse())
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ErrorResponse(ErrorDetail("PERMISSION_DENIED", e.message ?: "Permission denied", "authorization_error"))
                        )
                    }
                }
                
                /**
                 * Revoke access to a resource
                 * Requires admin permission on the resource
                 */
                delete("/{accessId}") {
                    val userId = call.getCurrentUserId() ?: return@delete call.respondUnauthorized()
                    val resourceType = call.parameters["resourceType"]
                        ?: return@delete call.respondBadRequest("Resource type required")
                    val resourceId = call.parameters["resourceId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    } ?: return@delete call.respondBadRequest("Invalid resource ID")
                    val accessId = call.parameters["accessId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    } ?: return@delete call.respondBadRequest("Invalid access ID")
                    val reason = call.request.queryParameters["reason"]
                    val ipAddress = getClientIp(call)
                    
                    try {
                        permissionService.revokeAccess(
                            revokerId = userId,
                            resourceAccessId = accessId,
                            reason = reason,
                            ipAddress = ipAddress
                        )
                        
                        call.respond(HttpStatusCode.NoContent)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(ErrorDetail("INVALID_REQUEST", e.message ?: "Cannot revoke access", "validation_error"))
                        )
                    }
                }
            }
            
            /**
             * Get current user's permission on a resource
             */
            route("/v1/resources/{resourceType}/{resourceId}/my-permission") {
                get {
                    val userId = call.getCurrentUserId() ?: return@get call.respondUnauthorized()
                    val resourceType = call.parameters["resourceType"]
                        ?: return@get call.respondBadRequest("Resource type required")
                    val resourceId = call.parameters["resourceId"]?.let { 
                        try { UUID.fromString(it) } catch (e: Exception) { null }
                    } ?: return@get call.respondBadRequest("Invalid resource ID")
                    
                    val permission = permissionService.getEffectivePermission(userId, resourceType, resourceId)
                    
                    // Determine access source
                    val accessSource = if (permission != null) {
                        // Check if it's direct or via role (simplified check)
                        val directAccess = permissionService.canAccessResource(userId, resourceType, resourceId, permission)
                        if (directAccess) "direct" else "role"
                    } else null
                    
                    call.respond(MyPermissionResponse(
                        permission = permission,
                        access_source = accessSource
                    ))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun ResourceAccessGrant.toResponse(): ResourceAccessResponse {
    // Look up user email and role name if available
    val userEmail = this.userId?.let { uid ->
        transaction {
            Users.select { Users.id eq uid }.firstOrNull()?.get(Users.email)
        }
    }
    
    val roleInfo = this.roleId?.let { rid ->
        transaction {
            Roles.select { Roles.id eq rid }.firstOrNull()?.let { row ->
                row[Roles.name] to row[Roles.displayName]
            }
        }
    }
    
    return ResourceAccessResponse(
        id = this.id.toString(),
        resource_type = this.resourceType,
        resource_id = this.resourceId.toString(),
        user_id = this.userId?.toString(),
        user_email = userEmail,
        role_id = this.roleId?.toString(),
        role_name = roleInfo?.first,
        role_display_name = roleInfo?.second,
        permission = this.permission,
        granted_by = this.grantedBy.toString(),
        granted_at = this.grantedAt.toString(),
        expires_at = this.expiresAt?.toString()
    )
}

