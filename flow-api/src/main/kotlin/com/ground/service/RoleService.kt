package com.ground.service

import com.ground.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * RoleService - Manages roles and role assignments.
 * 
 * Roles are named collections of permissions within a business.
 * Users are assigned roles to gain access to resources.
 */
class RoleService(
    private val auditService: AuditService = AuditService()
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    
    // ═══════════════════════════════════════════════════════════════
    // SYSTEM ROLES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * System role definitions.
     * These are automatically created for every business.
     */
    enum class SystemRole(
        val displayName: String,
        val description: String,
        val color: String,
        val businessPermissions: List<String>
    ) {
        OWNER(
            displayName = "Owner",
            description = "Full access to all business settings and resources. Cannot be removed.",
            color = "#dc2626",
            businessPermissions = listOf("*") // All permissions
        ),
        ADMIN(
            displayName = "Admin",
            description = "Can manage team members, roles, and business settings.",
            color = "#7c3aed",
            businessPermissions = listOf(
                "members:*",
                "roles:*",
                "settings:*",
                "audit:view"
            )
        ),
        DEVELOPER(
            displayName = "Developer",
            description = "Can create and manage applications and API resources.",
            color = "#2563eb",
            businessPermissions = listOf(
                "members:view",
                "roles:view"
            )
        ),
        ANALYST(
            displayName = "Analyst",
            description = "Read-only access for analytics and reporting.",
            color = "#059669",
            businessPermissions = listOf(
                "members:view"
            )
        ),
        VIEWER(
            displayName = "Viewer",
            description = "Basic read-only access.",
            color = "#6b7280",
            businessPermissions = listOf(
                "members:view"
            )
        )
    }
    
    /**
     * Create all system roles for a new business.
     * Returns the owner role ID (used to assign to creator).
     */
    fun createSystemRoles(businessId: UUID, createdBy: UUID): UUID {
        var ownerRoleId: UUID? = null
        
        transaction {
            val now = Instant.now()
            
            SystemRole.values().forEach { role ->
                val roleId = Roles.insertAndGetId {
                    it[Roles.businessId] = businessId
                    it[name] = role.name.lowercase()
                    it[displayName] = role.displayName
                    it[description] = role.description
                    it[color] = role.color
                    it[isSystem] = true
                    it[isDefault] = role == SystemRole.VIEWER // Viewer is default for new members
                    it[businessPermissions] = json.encodeToString(role.businessPermissions)
                    it[Roles.createdBy] = createdBy
                    it[createdAt] = now
                    it[updatedAt] = now
                }.value
                
                if (role == SystemRole.OWNER) {
                    ownerRoleId = roleId
                }
            }
        }
        
        return ownerRoleId ?: throw IllegalStateException("Failed to create owner role")
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ROLE CRUD
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create a custom role.
     */
    fun createRole(
        creatorId: UUID,
        businessId: UUID,
        name: String,
        displayName: String,
        description: String? = null,
        color: String? = null,
        businessPermissions: List<String> = emptyList(),
        ipAddress: String? = null
    ): RoleResponse {
        return transaction {
            val now = Instant.now()
            
            // Check if role name already exists in business
            val existing = Roles.select {
                (Roles.businessId eq businessId) and (Roles.name eq name.lowercase())
            }.firstOrNull()
            require(existing == null) { "Role '$name' already exists" }
            
            val roleId = Roles.insertAndGetId {
                it[Roles.businessId] = businessId
                it[Roles.name] = name.lowercase()
                it[Roles.displayName] = displayName
                it[Roles.description] = description
                it[Roles.color] = color ?: "#6b7280"
                it[isSystem] = false
                it[isDefault] = false
                it[Roles.businessPermissions] = json.encodeToString(businessPermissions)
                it[createdBy] = creatorId
                it[createdAt] = now
                it[updatedAt] = now
            }.value
            
            auditService.logRoleAction(
                actorUserId = creatorId,
                action = "role_created",
                roleId = roleId,
                businessId = businessId,
                metadata = mapOf("name" to name, "display_name" to displayName),
                ipAddress = ipAddress
            )
            
            getRoleResponse(roleId)!!
        }
    }
    
    /**
     * Get a role by ID.
     */
    fun getRole(roleId: UUID): RoleResponse? {
        return transaction {
            getRoleResponse(roleId)
        }
    }
    
    /**
     * List all roles for a business.
     */
    fun listRoles(businessId: UUID): List<RoleResponse> {
        return transaction {
            Roles.select { Roles.businessId eq businessId }
                .orderBy(Roles.isSystem to SortOrder.DESC, Roles.name to SortOrder.ASC)
                .map { row ->
                    RoleResponse(
                        id = row[Roles.id].value,
                        businessId = row[Roles.businessId],
                        name = row[Roles.name],
                        displayName = row[Roles.displayName],
                        description = row[Roles.description],
                        color = row[Roles.color],
                        isSystem = row[Roles.isSystem],
                        isDefault = row[Roles.isDefault],
                        businessPermissions = row[Roles.businessPermissions]?.let {
                            json.decodeFromString<List<String>>(it)
                        } ?: emptyList(),
                        memberCount = countRoleMembers(row[Roles.id].value),
                        createdAt = row[Roles.createdAt],
                        updatedAt = row[Roles.updatedAt]
                    )
                }
        }
    }
    
    /**
     * Update a custom role.
     * Cannot update system roles.
     */
    fun updateRole(
        actorId: UUID,
        roleId: UUID,
        displayName: String? = null,
        description: String? = null,
        color: String? = null,
        businessPermissions: List<String>? = null,
        ipAddress: String? = null
    ): RoleResponse? {
        return transaction {
            val role = Roles.select { Roles.id eq roleId }.firstOrNull()
                ?: return@transaction null
            
            require(!role[Roles.isSystem]) { "Cannot modify system roles" }
            
            Roles.update({ Roles.id eq roleId }) {
                displayName?.let { d -> it[Roles.displayName] = d }
                description?.let { d -> it[Roles.description] = d }
                color?.let { c -> it[Roles.color] = c }
                businessPermissions?.let { p -> it[Roles.businessPermissions] = json.encodeToString(p) }
                it[updatedAt] = Instant.now()
            }
            
            auditService.logRoleAction(
                actorUserId = actorId,
                action = "role_updated",
                roleId = roleId,
                businessId = role[Roles.businessId],
                metadata = mapOf(
                    "display_name" to displayName,
                    "description" to description
                ),
                ipAddress = ipAddress
            )
            
            getRoleResponse(roleId)
        }
    }
    
    /**
     * Delete a custom role.
     * Cannot delete system roles.
     */
    fun deleteRole(
        actorId: UUID,
        roleId: UUID,
        ipAddress: String? = null
    ): Boolean {
        return transaction {
            val role = Roles.select { Roles.id eq roleId }.firstOrNull()
                ?: return@transaction false
            
            require(!role[Roles.isSystem]) { "Cannot delete system roles" }
            
            val businessId = role[Roles.businessId]
            
            // First revoke all user role assignments
            UserRoles.update({ UserRoles.roleId eq roleId }) {
                it[revokedAt] = Instant.now()
                it[revokedBy] = actorId
            }
            
            // Revoke all resource access via this role
            ResourceAccess.update({ ResourceAccess.roleId eq roleId }) {
                it[revokedAt] = Instant.now()
                it[revokedBy] = actorId
            }
            
            // Delete the role
            Roles.deleteWhere { Roles.id eq roleId }
            
            auditService.logRoleAction(
                actorUserId = actorId,
                action = "role_deleted",
                roleId = roleId,
                businessId = businessId,
                metadata = mapOf("name" to role[Roles.name]),
                ipAddress = ipAddress
            )
            
            true
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ROLE ASSIGNMENT
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Assign a role to a user.
     */
    fun assignRole(
        actorId: UUID,
        userId: UUID,
        roleId: UUID,
        expiresAt: Instant? = null,
        ipAddress: String? = null
    ): UserRoleAssignment {
        return transaction {
            val role = Roles.select { Roles.id eq roleId }.first()
            val businessId = role[Roles.businessId]
            
            // Check if user is a member of the business
            val membership = BusinessMemberships.select {
                (BusinessMemberships.userId eq userId) and
                (BusinessMemberships.businessId eq businessId) and
                (BusinessMemberships.status eq "active")
            }.firstOrNull()
            require(membership != null) { "User is not a member of this business" }
            
            // Check if already assigned
            val existing = UserRoles.select {
                (UserRoles.userId eq userId) and
                (UserRoles.roleId eq roleId) and
                (UserRoles.revokedAt.isNull())
            }.firstOrNull()
            
            if (existing != null) {
                // Update expiration if provided
                if (expiresAt != null) {
                    UserRoles.update({ UserRoles.id eq existing[UserRoles.id] }) {
                        it[UserRoles.expiresAt] = expiresAt
                    }
                }
                return@transaction UserRoleAssignment(
                    id = existing[UserRoles.id].value,
                    userId = userId,
                    roleId = roleId,
                    businessId = businessId,
                    grantedBy = existing[UserRoles.grantedBy],
                    grantedAt = existing[UserRoles.grantedAt],
                    expiresAt = expiresAt ?: existing[UserRoles.expiresAt]
                )
            }
            
            val now = Instant.now()
            val assignmentId = UserRoles.insertAndGetId {
                it[UserRoles.userId] = userId
                it[UserRoles.roleId] = roleId
                it[UserRoles.businessId] = businessId
                it[grantedBy] = actorId
                it[grantedAt] = now
                it[UserRoles.expiresAt] = expiresAt
            }.value
            
            auditService.logRoleAction(
                actorUserId = actorId,
                action = "role_assigned",
                roleId = roleId,
                businessId = businessId,
                targetUserId = userId,
                metadata = mapOf(
                    "role_name" to role[Roles.name],
                    "expires_at" to expiresAt?.toString()
                ),
                ipAddress = ipAddress
            )
            
            UserRoleAssignment(
                id = assignmentId,
                userId = userId,
                roleId = roleId,
                businessId = businessId,
                grantedBy = actorId,
                grantedAt = now,
                expiresAt = expiresAt
            )
        }
    }
    
    /**
     * Revoke a role from a user.
     * Cannot revoke owner role from business owner.
     */
    fun revokeRole(
        actorId: UUID,
        userId: UUID,
        roleId: UUID,
        ipAddress: String? = null
    ) {
        transaction {
            val role = Roles.select { Roles.id eq roleId }.first()
            val businessId = role[Roles.businessId]
            
            // Cannot revoke owner role from business owner
            if (role[Roles.name] == "owner") {
                val business = Businesses.select { Businesses.id eq businessId }.first()
                require(business[Businesses.ownerId] != userId) {
                    "Cannot revoke owner role from business owner"
                }
            }
            
            UserRoles.update({
                (UserRoles.userId eq userId) and
                (UserRoles.roleId eq roleId) and
                (UserRoles.revokedAt.isNull())
            }) {
                it[revokedAt] = Instant.now()
                it[revokedBy] = actorId
            }
            
            auditService.logRoleAction(
                actorUserId = actorId,
                action = "role_revoked",
                roleId = roleId,
                businessId = businessId,
                targetUserId = userId,
                metadata = mapOf("role_name" to role[Roles.name]),
                ipAddress = ipAddress
            )
        }
    }
    
    /**
     * Get all roles for a user in a business.
     */
    fun getUserRoles(userId: UUID, businessId: UUID): List<RoleResponse> {
        return transaction {
            UserRoles
                .innerJoin(Roles)
                .select {
                    (UserRoles.userId eq userId) and
                    (UserRoles.businessId eq businessId) and
                    (UserRoles.revokedAt.isNull()) and
                    (UserRoles.expiresAt.isNull() or (UserRoles.expiresAt greater Instant.now()))
                }.map { row ->
                    RoleResponse(
                        id = row[Roles.id].value,
                        businessId = row[Roles.businessId],
                        name = row[Roles.name],
                        displayName = row[Roles.displayName],
                        description = row[Roles.description],
                        color = row[Roles.color],
                        isSystem = row[Roles.isSystem],
                        isDefault = row[Roles.isDefault],
                        businessPermissions = row[Roles.businessPermissions]?.let {
                            json.decodeFromString<List<String>>(it)
                        } ?: emptyList(),
                        memberCount = null,
                        createdAt = row[Roles.createdAt],
                        updatedAt = row[Roles.updatedAt]
                    )
                }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════
    
    private fun getRoleResponse(roleId: UUID): RoleResponse? {
        return Roles.select { Roles.id eq roleId }.firstOrNull()?.let { row ->
            RoleResponse(
                id = row[Roles.id].value,
                businessId = row[Roles.businessId],
                name = row[Roles.name],
                displayName = row[Roles.displayName],
                description = row[Roles.description],
                color = row[Roles.color],
                isSystem = row[Roles.isSystem],
                isDefault = row[Roles.isDefault],
                businessPermissions = row[Roles.businessPermissions]?.let {
                    json.decodeFromString<List<String>>(it)
                } ?: emptyList(),
                memberCount = countRoleMembers(roleId),
                createdAt = row[Roles.createdAt],
                updatedAt = row[Roles.updatedAt]
            )
        }
    }
    
    private fun countRoleMembers(roleId: UUID): Int {
        return UserRoles.select {
            (UserRoles.roleId eq roleId) and
            (UserRoles.revokedAt.isNull()) and
            (UserRoles.expiresAt.isNull() or (UserRoles.expiresAt greater Instant.now()))
        }.count().toInt()
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════

data class RoleResponse(
    val id: UUID,
    val businessId: UUID,
    val name: String,
    val displayName: String,
    val description: String?,
    val color: String?,
    val isSystem: Boolean,
    val isDefault: Boolean,
    val businessPermissions: List<String>,
    val memberCount: Int?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class UserRoleAssignment(
    val id: UUID,
    val userId: UUID,
    val roleId: UUID,
    val businessId: UUID,
    val grantedBy: UUID,
    val grantedAt: Instant,
    val expiresAt: Instant?
)

