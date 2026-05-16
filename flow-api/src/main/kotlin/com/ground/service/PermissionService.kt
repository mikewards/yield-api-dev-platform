package com.ground.service

import com.ground.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * PermissionService - THE CORE OF RCAC (Resource-Centric Access Control)
 * 
 * This service handles all permission checks and access grants.
 * 
 * Key principles:
 * 1. Resources are autonomous - they don't belong to businesses
 * 2. Access is explicit - every access requires a ResourceAccess record
 * 3. Access can be direct (to a user) OR via role (to all users with that role)
 * 4. Permissions are hierarchical: read < write < admin
 * 
 * Permission levels:
 * - "read": Can view the resource
 * - "write": Can view and modify the resource
 * - "admin": Can view, modify, delete, and share the resource
 */
class PermissionService(
    private val auditService: AuditService = AuditService()
) {
    
    // ═══════════════════════════════════════════════════════════════
    // RESOURCE ACCESS CHECKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Check if user can access a specific resource with required permission level.
     * This is the primary permission check used throughout the application.
     * 
     * @param userId The user attempting to access the resource
     * @param resourceType The type of resource (e.g., "application", "yield_account")
     * @param resourceId The specific resource's UUID
     * @param requiredPermission The minimum permission level needed ("read", "write", "admin")
     * @return true if user has sufficient permission, false otherwise
     */
    fun canAccessResource(
        userId: UUID,
        resourceType: String,
        resourceId: UUID,
        requiredPermission: String = "read"
    ): Boolean {
        return transaction {
            // 1. Check direct user access
            val directAccess = ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.resourceId eq resourceId) and
                (ResourceAccess.userId eq userId) and
                (ResourceAccess.revokedAt.isNull()) and
                (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
            }.firstOrNull()
            
            if (directAccess != null) {
                val userPermission = directAccess[ResourceAccess.permission]
                if (hasPermissionLevel(userPermission, requiredPermission)) {
                    return@transaction true
                }
            }
            
            // 2. Get all active roles for this user across all businesses
            val userRoleIds = UserRoles.select {
                (UserRoles.userId eq userId) and
                (UserRoles.revokedAt.isNull()) and
                (UserRoles.expiresAt.isNull() or (UserRoles.expiresAt greater Instant.now()))
            }.map { it[UserRoles.roleId] }
            
            if (userRoleIds.isEmpty()) return@transaction false
            
            // 3. Check if any of those roles grant access to this resource
            val roleAccess = ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.resourceId eq resourceId) and
                (ResourceAccess.roleId inList userRoleIds) and
                (ResourceAccess.revokedAt.isNull()) and
                (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
            }.toList()
            
            // Return true if any role grants sufficient permission
            roleAccess.any { row ->
                hasPermissionLevel(row[ResourceAccess.permission], requiredPermission)
            }
        }
    }
    
    /**
     * Get user's effective permission level for a resource.
     * Returns the highest permission level from all access grants.
     * 
     * @return The permission level ("read", "write", "admin") or null if no access
     */
    fun getEffectivePermission(
        userId: UUID,
        resourceType: String,
        resourceId: UUID
    ): String? {
        return transaction {
            val permissions = mutableListOf<String>()
            
            // Get direct access permission
            ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.resourceId eq resourceId) and
                (ResourceAccess.userId eq userId) and
                (ResourceAccess.revokedAt.isNull()) and
                (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
            }.firstOrNull()?.let {
                permissions.add(it[ResourceAccess.permission])
            }
            
            // Get role-based permissions
            val userRoleIds = UserRoles.select {
                (UserRoles.userId eq userId) and
                (UserRoles.revokedAt.isNull()) and
                (UserRoles.expiresAt.isNull() or (UserRoles.expiresAt greater Instant.now()))
            }.map { it[UserRoles.roleId] }
            
            if (userRoleIds.isNotEmpty()) {
                ResourceAccess.select {
                    (ResourceAccess.resourceType eq resourceType) and
                    (ResourceAccess.resourceId eq resourceId) and
                    (ResourceAccess.roleId inList userRoleIds) and
                    (ResourceAccess.revokedAt.isNull()) and
                    (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
                }.forEach {
                    permissions.add(it[ResourceAccess.permission])
                }
            }
            
            // Return highest permission level
            permissions.maxByOrNull { permissionLevel(it) }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // RESOURCE LISTING (What can user access?)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get all resource IDs of a type that user can access.
     * Used for listing resources (e.g., "list all applications I can see").
     * 
     * @param minimumPermission Filter to only include resources where user has at least this permission
     * @return List of resource IDs the user can access
     */
    fun getAccessibleResourceIds(
        userId: UUID,
        resourceType: String,
        minimumPermission: String = "read"
    ): List<UUID> {
        return transaction {
            val accessibleIds = mutableSetOf<UUID>()
            
            // Direct access
            ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.userId eq userId) and
                (ResourceAccess.revokedAt.isNull()) and
                (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
            }.filter { hasPermissionLevel(it[ResourceAccess.permission], minimumPermission) }
             .mapTo(accessibleIds) { it[ResourceAccess.resourceId] }
            
            // Role-based access
            val userRoleIds = UserRoles.select {
                (UserRoles.userId eq userId) and
                (UserRoles.revokedAt.isNull()) and
                (UserRoles.expiresAt.isNull() or (UserRoles.expiresAt greater Instant.now()))
            }.map { it[UserRoles.roleId] }
            
            if (userRoleIds.isNotEmpty()) {
                ResourceAccess.select {
                    (ResourceAccess.resourceType eq resourceType) and
                    (ResourceAccess.roleId inList userRoleIds) and
                    (ResourceAccess.revokedAt.isNull()) and
                    (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
                }.filter { hasPermissionLevel(it[ResourceAccess.permission], minimumPermission) }
                 .mapTo(accessibleIds) { it[ResourceAccess.resourceId] }
            }
            
            accessibleIds.toList()
        }
    }
    
    /**
     * Get all resources of all types that user can access, with permission details.
     * Used for dashboard views showing everything the user can see.
     */
    fun getAllAccessibleResources(
        userId: UUID,
        minimumPermission: String = "read"
    ): List<AccessibleResource> {
        return transaction {
            val results = mutableMapOf<Pair<String, UUID>, AccessibleResource>()
            
            // Get user's roles
            val userRoleIds = UserRoles.select {
                (UserRoles.userId eq userId) and
                (UserRoles.revokedAt.isNull()) and
                (UserRoles.expiresAt.isNull() or (UserRoles.expiresAt greater Instant.now()))
            }.map { it[UserRoles.roleId] }
            
            // Direct access
            ResourceAccess.select {
                (ResourceAccess.userId eq userId) and
                (ResourceAccess.revokedAt.isNull()) and
                (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
            }.filter { hasPermissionLevel(it[ResourceAccess.permission], minimumPermission) }
             .forEach { row ->
                val key = row[ResourceAccess.resourceType] to row[ResourceAccess.resourceId]
                val existing = results[key]
                val currentPerm = row[ResourceAccess.permission]
                
                // Keep highest permission
                if (existing == null || permissionLevel(currentPerm) > permissionLevel(existing.permission)) {
                    results[key] = AccessibleResource(
                        resourceType = row[ResourceAccess.resourceType],
                        resourceId = row[ResourceAccess.resourceId],
                        permission = currentPerm,
                        accessSource = AccessSource.DIRECT
                    )
                }
            }
            
            // Role-based access
            if (userRoleIds.isNotEmpty()) {
                ResourceAccess.select {
                    (ResourceAccess.roleId inList userRoleIds) and
                    (ResourceAccess.revokedAt.isNull()) and
                    (ResourceAccess.expiresAt.isNull() or (ResourceAccess.expiresAt greater Instant.now()))
                }.filter { hasPermissionLevel(it[ResourceAccess.permission], minimumPermission) }
                 .forEach { row ->
                    val key = row[ResourceAccess.resourceType] to row[ResourceAccess.resourceId]
                    val existing = results[key]
                    val currentPerm = row[ResourceAccess.permission]
                    
                    // Keep highest permission, prefer direct access source
                    if (existing == null || 
                        (permissionLevel(currentPerm) > permissionLevel(existing.permission))) {
                        results[key] = AccessibleResource(
                            resourceType = row[ResourceAccess.resourceType],
                            resourceId = row[ResourceAccess.resourceId],
                            permission = currentPerm,
                            accessSource = if (existing?.accessSource == AccessSource.DIRECT) AccessSource.DIRECT else AccessSource.ROLE,
                            roleId = row[ResourceAccess.roleId]
                        )
                    }
                }
            }
            
            results.values.toList()
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ACCESS GRANTING
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Grant a user direct access to a resource.
     * Requires admin permission on the resource.
     * 
     * @throws IllegalArgumentException if grantor doesn't have admin permission
     */
    fun grantUserAccess(
        grantorId: UUID,
        targetUserId: UUID,
        resourceType: String,
        resourceId: UUID,
        permission: String,
        reason: String? = null,
        expiresAt: Instant? = null,
        ipAddress: String? = null
    ): ResourceAccessGrant {
        return transaction {
            // Verify grantor has admin permission on this resource
            require(canAccessResource(grantorId, resourceType, resourceId, "admin")) {
                "You must have admin permission to grant access"
            }
            
            // Can only grant permissions up to your own level
            val grantorPermission = getEffectivePermission(grantorId, resourceType, resourceId)
            require(grantorPermission != null && hasPermissionLevel(grantorPermission, permission)) {
                "Cannot grant permission higher than your own"
            }
            
            // Check if access already exists
            val existing = ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.resourceId eq resourceId) and
                (ResourceAccess.userId eq targetUserId) and
                (ResourceAccess.revokedAt.isNull())
            }.firstOrNull()
            
            val accessId: UUID
            val action: String
            
            if (existing != null) {
                // Update existing access
                accessId = existing[ResourceAccess.id].value
                action = "access_updated"
                
                ResourceAccess.update({
                    ResourceAccess.id eq existing[ResourceAccess.id]
                }) {
                    it[ResourceAccess.permission] = permission
                    it[ResourceAccess.expiresAt] = expiresAt
                    it[ResourceAccess.grantedBy] = grantorId
                    it[ResourceAccess.grantedAt] = Instant.now()
                    it[ResourceAccess.grantReason] = reason
                }
            } else {
                // Create new access
                action = "access_granted"
                accessId = ResourceAccess.insertAndGetId {
                    it[ResourceAccess.resourceType] = resourceType
                    it[ResourceAccess.resourceId] = resourceId
                    it[ResourceAccess.userId] = targetUserId
                    it[ResourceAccess.permission] = permission
                    it[ResourceAccess.grantedBy] = grantorId
                    it[ResourceAccess.grantedAt] = Instant.now()
                    it[ResourceAccess.grantReason] = reason
                    it[ResourceAccess.expiresAt] = expiresAt
                }.value
            }
            
            // Audit log
            auditService.logResourceAccessAction(
                actorUserId = grantorId,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                accessId = accessId,
                metadata = mapOf(
                    "target_user_id" to targetUserId.toString(),
                    "permission" to permission,
                    "reason" to reason
                ),
                ipAddress = ipAddress
            )
            
            ResourceAccessGrant(
                id = accessId,
                resourceType = resourceType,
                resourceId = resourceId,
                userId = targetUserId,
                roleId = null,
                permission = permission,
                grantedBy = grantorId,
                grantedAt = Instant.now(),
                expiresAt = expiresAt
            )
        }
    }
    
    /**
     * Grant a role access to a resource.
     * All users with that role will gain access.
     * Requires admin permission on the resource.
     */
    fun grantRoleAccess(
        grantorId: UUID,
        roleId: UUID,
        resourceType: String,
        resourceId: UUID,
        permission: String,
        reason: String? = null,
        ipAddress: String? = null
    ): ResourceAccessGrant {
        return transaction {
            // Verify grantor has admin permission
            require(canAccessResource(grantorId, resourceType, resourceId, "admin")) {
                "You must have admin permission to grant access"
            }
            
            // Can only grant permissions up to your own level
            val grantorPermission = getEffectivePermission(grantorId, resourceType, resourceId)
            require(grantorPermission != null && hasPermissionLevel(grantorPermission, permission)) {
                "Cannot grant permission higher than your own"
            }
            
            // Check if access already exists
            val existing = ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.resourceId eq resourceId) and
                (ResourceAccess.roleId eq roleId) and
                (ResourceAccess.revokedAt.isNull())
            }.firstOrNull()
            
            val accessId: UUID
            val action: String
            
            if (existing != null) {
                // Update existing access
                accessId = existing[ResourceAccess.id].value
                action = "access_updated"
                
                ResourceAccess.update({
                    ResourceAccess.id eq existing[ResourceAccess.id]
                }) {
                    it[ResourceAccess.permission] = permission
                    it[ResourceAccess.grantedBy] = grantorId
                    it[ResourceAccess.grantedAt] = Instant.now()
                    it[ResourceAccess.grantReason] = reason
                }
            } else {
                // Create new access
                action = "access_granted"
                accessId = ResourceAccess.insertAndGetId {
                    it[ResourceAccess.resourceType] = resourceType
                    it[ResourceAccess.resourceId] = resourceId
                    it[ResourceAccess.roleId] = roleId
                    it[ResourceAccess.permission] = permission
                    it[ResourceAccess.grantedBy] = grantorId
                    it[ResourceAccess.grantedAt] = Instant.now()
                    it[ResourceAccess.grantReason] = reason
                }.value
            }
            
            // Get role details for audit
            val role = Roles.select { Roles.id eq roleId }.first()
            val businessId = role[Roles.businessId]
            
            auditService.logResourceAccessAction(
                actorUserId = grantorId,
                action = action,
                resourceType = resourceType,
                resourceId = resourceId,
                accessId = accessId,
                metadata = mapOf(
                    "target_role_id" to roleId.toString(),
                    "target_role_name" to role[Roles.name],
                    "business_id" to businessId.toString(),
                    "permission" to permission,
                    "reason" to reason
                ),
                ipAddress = ipAddress
            )
            
            ResourceAccessGrant(
                id = accessId,
                resourceType = resourceType,
                resourceId = resourceId,
                userId = null,
                roleId = roleId,
                permission = permission,
                grantedBy = grantorId,
                grantedAt = Instant.now(),
                expiresAt = null
            )
        }
    }
    
    /**
     * Revoke access to a resource.
     * Requires admin permission on the resource.
     */
    fun revokeAccess(
        revokerId: UUID,
        resourceAccessId: UUID,
        reason: String? = null,
        ipAddress: String? = null
    ) {
        transaction {
            val access = ResourceAccess.select { ResourceAccess.id eq resourceAccessId }
                .firstOrNull() ?: throw IllegalArgumentException("Access grant not found")
            
            val resourceType = access[ResourceAccess.resourceType]
            val resourceId = access[ResourceAccess.resourceId]
            
            // Verify revoker has admin permission
            require(canAccessResource(revokerId, resourceType, resourceId, "admin")) {
                "You must have admin permission to revoke access"
            }
            
            // Cannot revoke your own admin access if you're the last admin
            val targetUserId = access[ResourceAccess.userId]
            if (targetUserId == revokerId && access[ResourceAccess.permission] == "admin") {
                val otherAdmins = countAdmins(resourceType, resourceId, excludeAccessId = resourceAccessId)
                require(otherAdmins > 0) {
                    "Cannot remove last admin. Transfer admin access to another user first."
                }
            }
            
            ResourceAccess.update({ ResourceAccess.id eq resourceAccessId }) {
                it[revokedAt] = Instant.now()
                it[revokedBy] = revokerId
                it[revokeReason] = reason
            }
            
            auditService.logResourceAccessAction(
                actorUserId = revokerId,
                action = "access_revoked",
                resourceType = resourceType,
                resourceId = resourceId,
                accessId = resourceAccessId,
                metadata = mapOf(
                    "revoked_user_id" to access[ResourceAccess.userId]?.toString(),
                    "revoked_role_id" to access[ResourceAccess.roleId]?.toString(),
                    "reason" to reason
                ),
                ipAddress = ipAddress
            )
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ACCESS LISTING (Who has access to this resource?)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Get all access grants for a resource.
     * Requires admin permission on the resource.
     */
    fun getResourceAccessList(
        userId: UUID,
        resourceType: String,
        resourceId: UUID
    ): List<ResourceAccessGrant> {
        return transaction {
            // Verify user has admin permission
            require(canAccessResource(userId, resourceType, resourceId, "admin")) {
                "Admin permission required to view access list"
            }
            
            ResourceAccess.select {
                (ResourceAccess.resourceType eq resourceType) and
                (ResourceAccess.resourceId eq resourceId) and
                (ResourceAccess.revokedAt.isNull())
            }.map { row ->
                ResourceAccessGrant(
                    id = row[ResourceAccess.id].value,
                    resourceType = row[ResourceAccess.resourceType],
                    resourceId = row[ResourceAccess.resourceId],
                    userId = row[ResourceAccess.userId],
                    roleId = row[ResourceAccess.roleId],
                    permission = row[ResourceAccess.permission],
                    grantedBy = row[ResourceAccess.grantedBy],
                    grantedAt = row[ResourceAccess.grantedAt],
                    expiresAt = row[ResourceAccess.expiresAt]
                )
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // AUTO-GRANT ON RESOURCE CREATION
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Auto-grant admin access to creator when a resource is created.
     * This should be called immediately after creating any resource.
     */
    fun grantCreatorAccess(
        creatorId: UUID,
        resourceType: String,
        resourceId: UUID
    ): ResourceAccessGrant {
        return transaction {
            val accessId = ResourceAccess.insertAndGetId {
                it[ResourceAccess.resourceType] = resourceType
                it[ResourceAccess.resourceId] = resourceId
                it[userId] = creatorId
                it[permission] = "admin"
                it[grantedBy] = creatorId
                it[grantedAt] = Instant.now()
                it[grantReason] = "Creator automatically granted admin access"
            }.value
            
            auditService.logResourceAccessAction(
                actorUserId = creatorId,
                action = "access_granted",
                resourceType = resourceType,
                resourceId = resourceId,
                accessId = accessId,
                metadata = mapOf(
                    "target_user_id" to creatorId.toString(),
                    "permission" to "admin",
                    "reason" to "auto_creator"
                )
            )
            
            ResourceAccessGrant(
                id = accessId,
                resourceType = resourceType,
                resourceId = resourceId,
                userId = creatorId,
                roleId = null,
                permission = "admin",
                grantedBy = creatorId,
                grantedAt = Instant.now(),
                expiresAt = null
            )
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Permission level as integer for comparison.
     * Higher number = more permission.
     */
    private fun permissionLevel(permission: String): Int = when (permission) {
        "read" -> 1
        "write" -> 2
        "admin" -> 3
        else -> 0
    }
    
    /**
     * Check if actual permission meets or exceeds required permission.
     */
    private fun hasPermissionLevel(actual: String, required: String): Boolean {
        return permissionLevel(actual) >= permissionLevel(required)
    }
    
    /**
     * Count admins for a resource, optionally excluding a specific access grant.
     */
    private fun countAdmins(
        resourceType: String,
        resourceId: UUID,
        excludeAccessId: UUID? = null
    ): Int {
        return ResourceAccess.select {
            (ResourceAccess.resourceType eq resourceType) and
            (ResourceAccess.resourceId eq resourceId) and
            (ResourceAccess.permission eq "admin") and
            (ResourceAccess.revokedAt.isNull()) and
            (if (excludeAccessId != null) ResourceAccess.id neq excludeAccessId else Op.TRUE)
        }.count().toInt()
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════

/**
 * Represents a single access grant (returned from queries)
 */
data class ResourceAccessGrant(
    val id: UUID,
    val resourceType: String,
    val resourceId: UUID,
    val userId: UUID?,
    val roleId: UUID?,
    val permission: String,
    val grantedBy: UUID,
    val grantedAt: Instant,
    val expiresAt: Instant?
)

/**
 * Represents a resource the user can access (for listing)
 */
data class AccessibleResource(
    val resourceType: String,
    val resourceId: UUID,
    val permission: String,
    val accessSource: AccessSource,
    val roleId: UUID? = null
)

/**
 * How the user gained access to a resource
 */
enum class AccessSource {
    DIRECT,  // User was granted access directly
    ROLE     // User has access via a role
}

