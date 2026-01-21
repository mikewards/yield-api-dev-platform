package com.ground.service

import com.ground.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * BusinessService - Manages businesses (organizations) and memberships.
 * 
 * Businesses are containers for:
 * - Grouping users (via BusinessMemberships)
 * - Defining roles (via Roles)
 * - Billing context
 * 
 * IMPORTANT: Businesses do NOT own resources! Resources are autonomous.
 */
class BusinessService(
    private val auditService: AuditService = AuditService(),
    private val roleService: RoleService = RoleService()
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // ═══════════════════════════════════════════════════════════════
    // BUSINESS CRUD
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create a new business.
     * The creating user becomes the owner with full access.
     */
    fun createBusiness(
        userId: UUID,
        name: String,
        slug: String? = null,
        description: String? = null,
        website: String? = null,
        ipAddress: String? = null
    ): BusinessResponse {
        return transaction {
            val now = Instant.now()
            val businessSlug = slug ?: generateSlug(name)
            
            // Check if slug is taken
            val existingSlug = Businesses.select { Businesses.slug eq businessSlug }.firstOrNull()
            require(existingSlug == null) { "Business slug '$businessSlug' is already taken" }
            
            // Create business
            val businessId = Businesses.insertAndGetId {
                it[Businesses.name] = name
                it[Businesses.slug] = businessSlug
                it[Businesses.description] = description
                it[Businesses.website] = website
                it[ownerId] = userId
                it[status] = "active"
                it[plan] = "free"
                it[createdAt] = now
                it[updatedAt] = now
            }.value
            
            // Create owner membership (auto-accepted)
            BusinessMemberships.insert {
                it[BusinessMemberships.userId] = userId
                it[BusinessMemberships.businessId] = businessId
                it[BusinessMemberships.status] = "active"
                it[acceptedAt] = now
                it[createdAt] = now
            }
            
            // Create system roles for the business
            val ownerRoleId = roleService.createSystemRoles(businessId, userId)
            
            // Assign owner role to creator
            UserRoles.insert {
                it[UserRoles.userId] = userId
                it[roleId] = ownerRoleId
                it[UserRoles.businessId] = businessId
                it[grantedBy] = userId
                it[grantedAt] = now
            }
            
            auditService.logBusinessAction(
                actorUserId = userId,
                action = "business_created",
                businessId = businessId,
                metadata = mapOf("name" to name, "slug" to businessSlug),
                ipAddress = ipAddress
            )
            
            getBusinessResponse(businessId)!!
        }
    }
    
    /**
     * Get a business by ID.
     */
    fun getBusiness(businessId: UUID): BusinessResponse? {
        return transaction {
            getBusinessResponse(businessId)
        }
    }
    
    /**
     * List all businesses the user is a member of.
     */
    fun listUserBusinesses(userId: UUID): List<BusinessWithRoles> {
        return transaction {
            // Get all active memberships
            val memberships = BusinessMemberships
                .innerJoin(Businesses)
                .select {
                    (BusinessMemberships.userId eq userId) and
                    (BusinessMemberships.status eq "active")
                }.toList()
            
            memberships.map { row ->
                val businessId = row[Businesses.id].value
                
                // Get user's roles in this business
                val roles = UserRoles
                    .innerJoin(Roles)
                    .select {
                        (UserRoles.userId eq userId) and
                        (UserRoles.businessId eq businessId) and
                        (UserRoles.revokedAt.isNull())
                    }.map { roleRow ->
                        RoleInfo(
                            id = roleRow[Roles.id].value,
                            name = roleRow[Roles.name],
                            displayName = roleRow[Roles.displayName],
                            color = roleRow[Roles.color],
                            isSystem = roleRow[Roles.isSystem]
                        )
                    }
                
                BusinessWithRoles(
                    id = businessId,
                    name = row[Businesses.name],
                    slug = row[Businesses.slug],
                    logoUrl = row[Businesses.logoUrl],
                    description = row[Businesses.description],
                    website = row[Businesses.website],
                    plan = row[Businesses.plan],
                    status = row[Businesses.status],
                    isOwner = row[Businesses.ownerId] == userId,
                    roles = roles,
                    createdAt = row[Businesses.createdAt],
                    updatedAt = row[Businesses.updatedAt]
                )
            }
        }
    }
    
    /**
     * Update a business.
     * Requires owner or admin role.
     */
    fun updateBusiness(
        userId: UUID,
        businessId: UUID,
        name: String? = null,
        description: String? = null,
        website: String? = null,
        logoUrl: String? = null,
        ipAddress: String? = null
    ): BusinessResponse? {
        return transaction {
            // Check if user has permission (owner or admin role)
            require(hasBusinessPermission(userId, businessId, "settings:write")) {
                "You don't have permission to update this business"
            }
            
            val updated = Businesses.update({ Businesses.id eq businessId }) {
                name?.let { n -> it[Businesses.name] = n }
                description?.let { d -> it[Businesses.description] = d }
                website?.let { w -> it[Businesses.website] = w }
                logoUrl?.let { l -> it[Businesses.logoUrl] = l }
                it[updatedAt] = Instant.now()
            }
            
            if (updated > 0) {
                auditService.logBusinessAction(
                    actorUserId = userId,
                    action = "business_updated",
                    businessId = businessId,
                    metadata = mapOf(
                        "name" to name,
                        "description" to description,
                        "website" to website
                    ),
                    ipAddress = ipAddress
                )
                getBusinessResponse(businessId)
            } else {
                null
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MEMBERSHIP MANAGEMENT
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Invite a user to a business by email.
     * Creates a pending membership and returns an invitation token.
     */
    fun inviteMember(
        inviterId: UUID,
        businessId: UUID,
        email: String,
        roleIds: List<UUID>,
        ipAddress: String? = null
    ): InvitationResponse {
        return transaction {
            // Check if inviter has permission
            require(hasBusinessPermission(inviterId, businessId, "members:invite")) {
                "You don't have permission to invite members"
            }
            
            // Check if user with this email already exists
            val existingUser = Users.select { Users.email eq email.lowercase() }.firstOrNull()
            val targetUserId = existingUser?.get(Users.id)?.value
            
            // Check if already a member
            if (targetUserId != null) {
                val existingMembership = BusinessMemberships.select {
                    (BusinessMemberships.userId eq targetUserId) and
                    (BusinessMemberships.businessId eq businessId) and
                    (BusinessMemberships.status eq "active")
                }.firstOrNull()
                
                require(existingMembership == null) { "User is already a member of this business" }
            }
            
            val now = Instant.now()
            val token = generateInvitationToken()
            val expiresAt = now.plus(7, ChronoUnit.DAYS)
            
            // Create or update membership invitation
            val membershipId = if (targetUserId != null) {
                // User exists, create membership with their ID
                val existing = BusinessMemberships.select {
                    (BusinessMemberships.userId eq targetUserId) and
                    (BusinessMemberships.businessId eq businessId)
                }.firstOrNull()
                
                if (existing != null) {
                    // Update existing revoked membership
                    BusinessMemberships.update({
                        BusinessMemberships.id eq existing[BusinessMemberships.id]
                    }) {
                        it[status] = "pending"
                        it[invitedBy] = inviterId
                        it[invitedAt] = now
                        it[invitationToken] = token
                        it[invitationExpiresAt] = expiresAt
                        it[invitationEmail] = email.lowercase()
                        it[revokedAt] = null
                        it[revokedBy] = null
                    }
                    existing[BusinessMemberships.id].value
                } else {
                    BusinessMemberships.insertAndGetId {
                        it[userId] = targetUserId
                        it[BusinessMemberships.businessId] = businessId
                        it[status] = "pending"
                        it[invitedBy] = inviterId
                        it[invitedAt] = now
                        it[invitationToken] = token
                        it[invitationExpiresAt] = expiresAt
                        it[invitationEmail] = email.lowercase()
                        it[createdAt] = now
                    }.value
                }
            } else {
                // User doesn't exist yet, create invitation-only membership
                // They'll claim it when they register
                BusinessMemberships.insertAndGetId {
                    // userId will be set when they accept
                    it[BusinessMemberships.businessId] = businessId
                    it[status] = "pending"
                    it[invitedBy] = inviterId
                    it[invitedAt] = now
                    it[invitationToken] = token
                    it[invitationExpiresAt] = expiresAt
                    it[invitationEmail] = email.lowercase()
                    it[createdAt] = now
                }.value
            }
            
            // Pre-assign roles (will be activated on acceptance)
            roleIds.forEach { roleId ->
                // Verify role belongs to this business
                val role = Roles.select { 
                    (Roles.id eq roleId) and (Roles.businessId eq businessId) 
                }.firstOrNull()
                
                if (role != null && targetUserId != null) {
                    // Only create UserRole if user exists
                    val existingUserRole = UserRoles.select {
                        (UserRoles.userId eq targetUserId) and (UserRoles.roleId eq roleId)
                    }.firstOrNull()
                    
                    if (existingUserRole == null) {
                        UserRoles.insert {
                            it[userId] = targetUserId
                            it[UserRoles.roleId] = roleId
                            it[UserRoles.businessId] = businessId
                            it[grantedBy] = inviterId
                            it[grantedAt] = now
                            // Mark as not yet active (will activate on membership acceptance)
                        }
                    }
                }
            }
            
            auditService.logMembershipAction(
                actorUserId = inviterId,
                action = "membership_invited",
                businessId = businessId,
                targetUserId = targetUserId ?: inviterId, // Use inviter as placeholder if user doesn't exist
                metadata = mapOf(
                    "email" to email,
                    "role_ids" to roleIds.joinToString(",")
                ),
                ipAddress = ipAddress
            )
            
            val business = Businesses.select { Businesses.id eq businessId }.first()
            
            InvitationResponse(
                membershipId = membershipId,
                businessId = businessId,
                businessName = business[Businesses.name],
                email = email,
                token = token,
                expiresAt = expiresAt,
                roleIds = roleIds
            )
        }
    }
    
    /**
     * Accept an invitation to join a business.
     */
    fun acceptInvitation(
        userId: UUID,
        token: String,
        ipAddress: String? = null
    ): BusinessWithRoles {
        return transaction {
            val now = Instant.now()
            
            // Find the invitation
            val invitation = BusinessMemberships.select {
                (BusinessMemberships.invitationToken eq token) and
                (BusinessMemberships.status eq "pending")
            }.firstOrNull() ?: throw IllegalArgumentException("Invalid or expired invitation")
            
            // Check expiration
            val expiresAt = invitation[BusinessMemberships.invitationExpiresAt]
            require(expiresAt == null || expiresAt.isAfter(now)) { "Invitation has expired" }
            
            // Verify the user's email matches the invitation (if they were invited by email)
            val invitationEmail = invitation[BusinessMemberships.invitationEmail]
            if (invitationEmail != null) {
                val user = Users.select { Users.id eq userId }.first()
                require(user[Users.email].lowercase() == invitationEmail.lowercase()) {
                    "This invitation was sent to a different email address"
                }
            }
            
            val businessId = invitation[BusinessMemberships.businessId]
            val membershipId = invitation[BusinessMemberships.id].value
            
            // Update membership
            BusinessMemberships.update({ BusinessMemberships.id eq invitation[BusinessMemberships.id] }) {
                it[BusinessMemberships.userId] = userId
                it[status] = "active"
                it[acceptedAt] = now
                it[invitationToken] = null
                it[invitationExpiresAt] = null
            }
            
            auditService.logMembershipAction(
                actorUserId = userId,
                action = "membership_accepted",
                businessId = businessId,
                targetUserId = userId,
                ipAddress = ipAddress
            )
            
            // Return the business with user's roles
            listUserBusinesses(userId).find { it.id == businessId }
                ?: throw IllegalStateException("Failed to load business after accepting invitation")
        }
    }
    
    /**
     * Decline an invitation.
     */
    fun declineInvitation(
        userId: UUID,
        token: String,
        ipAddress: String? = null
    ) {
        transaction {
            val invitation = BusinessMemberships.select {
                (BusinessMemberships.invitationToken eq token) and
                (BusinessMemberships.status eq "pending")
            }.firstOrNull() ?: throw IllegalArgumentException("Invalid invitation")
            
            val businessId = invitation[BusinessMemberships.businessId]
            
            // Delete the pending membership
            BusinessMemberships.deleteWhere { 
                BusinessMemberships.id eq invitation[BusinessMemberships.id] 
            }
            
            auditService.logMembershipAction(
                actorUserId = userId,
                action = "membership_declined",
                businessId = businessId,
                targetUserId = userId,
                ipAddress = ipAddress
            )
        }
    }
    
    /**
     * Remove a member from a business.
     * Cannot remove the owner.
     */
    fun removeMember(
        actorId: UUID,
        businessId: UUID,
        targetUserId: UUID,
        ipAddress: String? = null
    ) {
        transaction {
            // Check permission
            require(hasBusinessPermission(actorId, businessId, "members:remove")) {
                "You don't have permission to remove members"
            }
            
            // Cannot remove owner
            val business = Businesses.select { Businesses.id eq businessId }.first()
            require(business[Businesses.ownerId] != targetUserId) {
                "Cannot remove the business owner"
            }
            
            val now = Instant.now()
            
            // Revoke membership
            BusinessMemberships.update({
                (BusinessMemberships.userId eq targetUserId) and
                (BusinessMemberships.businessId eq businessId)
            }) {
                it[status] = "revoked"
                it[revokedAt] = now
                it[revokedBy] = actorId
            }
            
            // Revoke all roles
            UserRoles.update({
                (UserRoles.userId eq targetUserId) and
                (UserRoles.businessId eq businessId)
            }) {
                it[revokedAt] = now
                it[revokedBy] = actorId
            }
            
            auditService.logMembershipAction(
                actorUserId = actorId,
                action = "membership_revoked",
                businessId = businessId,
                targetUserId = targetUserId,
                ipAddress = ipAddress
            )
        }
    }
    
    /**
     * List members of a business.
     */
    fun listMembers(userId: UUID, businessId: UUID): List<MemberInfo> {
        return transaction {
            // Check permission
            require(hasBusinessPermission(userId, businessId, "members:view")) {
                "You don't have permission to view members"
            }
            
            BusinessMemberships
                .innerJoin(Users)
                .select {
                    (BusinessMemberships.businessId eq businessId) and
                    (BusinessMemberships.status eq "active")
                }.map { row ->
                    val memberId = row[Users.id].value
                    
                    val roles = UserRoles
                        .innerJoin(Roles)
                        .select {
                            (UserRoles.userId eq memberId) and
                            (UserRoles.businessId eq businessId) and
                            (UserRoles.revokedAt.isNull())
                        }.map { roleRow ->
                            RoleInfo(
                                id = roleRow[Roles.id].value,
                                name = roleRow[Roles.name],
                                displayName = roleRow[Roles.displayName],
                                color = roleRow[Roles.color],
                                isSystem = roleRow[Roles.isSystem]
                            )
                        }
                    
                    val business = Businesses.select { Businesses.id eq businessId }.first()
                    
                    MemberInfo(
                        userId = memberId,
                        email = row[Users.email],
                        firstName = row[Users.firstName],
                        lastName = row[Users.lastName],
                        avatarUrl = row[Users.avatarUrl],
                        roles = roles,
                        isOwner = business[Businesses.ownerId] == memberId,
                        joinedAt = row[BusinessMemberships.acceptedAt]
                    )
                }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PERMISSION CHECKS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Check if user has a specific business-level permission.
     */
    fun hasBusinessPermission(userId: UUID, businessId: UUID, permission: String): Boolean {
        return transaction {
            // Owner has all permissions
            val business = Businesses.select { Businesses.id eq businessId }.firstOrNull()
                ?: return@transaction false
            
            if (business[Businesses.ownerId] == userId) return@transaction true
            
            // Check membership
            val membership = BusinessMemberships.select {
                (BusinessMemberships.userId eq userId) and
                (BusinessMemberships.businessId eq businessId) and
                (BusinessMemberships.status eq "active")
            }.firstOrNull() ?: return@transaction false
            
            // Get user's roles
            val userRoles = UserRoles
                .innerJoin(Roles)
                .select {
                    (UserRoles.userId eq userId) and
                    (UserRoles.businessId eq businessId) and
                    (UserRoles.revokedAt.isNull())
                }.toList()
            
            // Check if any role grants the permission
            userRoles.any { roleRow ->
                val permissions = roleRow[Roles.businessPermissions]?.let {
                    json.decodeFromString<List<String>>(it)
                } ?: emptyList()
                
                // Check for exact match or wildcard
                permissions.any { p ->
                    p == permission || 
                    p == "*" || 
                    (p.endsWith(":*") && permission.startsWith(p.dropLast(1)))
                }
            }
        }
    }
    
    /**
     * Check if user is a member of a business.
     */
    fun isMember(userId: UUID, businessId: UUID): Boolean {
        return transaction {
            BusinessMemberships.select {
                (BusinessMemberships.userId eq userId) and
                (BusinessMemberships.businessId eq businessId) and
                (BusinessMemberships.status eq "active")
            }.firstOrNull() != null
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════
    
    private fun getBusinessResponse(businessId: UUID): BusinessResponse? {
        return Businesses.select { Businesses.id eq businessId }.firstOrNull()?.let { row ->
            BusinessResponse(
                id = row[Businesses.id].value,
                name = row[Businesses.name],
                slug = row[Businesses.slug],
                logoUrl = row[Businesses.logoUrl],
                description = row[Businesses.description],
                website = row[Businesses.website],
                plan = row[Businesses.plan],
                status = row[Businesses.status],
                ownerId = row[Businesses.ownerId],
                createdAt = row[Businesses.createdAt],
                updatedAt = row[Businesses.updatedAt]
            )
        }
    }
    
    private fun generateSlug(name: String): String {
        val base = name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .take(50)
        
        // Add random suffix to ensure uniqueness
        val suffix = UUID.randomUUID().toString().take(6)
        return "$base-$suffix"
    }
    
    private fun generateInvitationToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════

data class BusinessResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val logoUrl: String?,
    val description: String?,
    val website: String?,
    val plan: String,
    val status: String,
    val ownerId: UUID,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class BusinessWithRoles(
    val id: UUID,
    val name: String,
    val slug: String,
    val logoUrl: String?,
    val description: String?,
    val website: String?,
    val plan: String,
    val status: String,
    val isOwner: Boolean,
    val roles: List<RoleInfo>,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

data class RoleInfo(
    val id: UUID,
    val name: String,
    val displayName: String,
    val color: String?,
    val isSystem: Boolean
)

data class MemberInfo(
    val userId: UUID,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
    val roles: List<RoleInfo>,
    val isOwner: Boolean,
    val joinedAt: Instant?
)

data class InvitationResponse(
    val membershipId: UUID,
    val businessId: UUID,
    val businessName: String,
    val email: String,
    val token: String,
    val expiresAt: Instant,
    val roleIds: List<UUID>
)

// ═══════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS TO CONVERT TO DTOs
// ═══════════════════════════════════════════════════════════════

fun BusinessResponse.toDto(): com.ground.dto.BusinessResponse = com.ground.dto.BusinessResponse(
    id = id.toString(),
    name = name,
    slug = slug,
    logo_url = logoUrl,
    description = description,
    website = website,
    plan = plan,
    status = status,
    owner_id = ownerId.toString(),
    created_at = createdAt?.toString() ?: "",
    updated_at = updatedAt?.toString()
)

fun BusinessWithRoles.toBusinessWithRolesResponse(): com.ground.dto.BusinessWithRolesResponse = com.ground.dto.BusinessWithRolesResponse(
    id = id.toString(),
    name = name,
    slug = slug,
    logo_url = logoUrl,
    description = description,
    website = website,
    plan = plan,
    status = status,
    is_owner = isOwner,
    roles = roles.map { it.toRoleInfoResponse() },
    created_at = createdAt?.toString() ?: "",
    updated_at = updatedAt?.toString()
)

fun RoleInfo.toRoleInfoResponse(): com.ground.dto.RoleInfoResponse = com.ground.dto.RoleInfoResponse(
    id = id.toString(),
    name = name,
    display_name = displayName,
    color = color,
    is_system = isSystem
)

