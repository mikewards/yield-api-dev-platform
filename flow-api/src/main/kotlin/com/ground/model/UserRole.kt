package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * UserRoles - Assigns roles to users within a business context.
 * 
 * This is the many-to-many link between Users and Roles.
 * A user can have multiple roles within the same business.
 * A user can have roles in multiple businesses.
 * 
 * businessId is denormalized here for efficient queries:
 * "What roles does user X have in business Y?"
 * 
 * Supports:
 * - Time-limited access via expiresAt
 * - Audit trail via grantedBy/grantedAt
 * - Revocation tracking
 */
object UserRoles : UUIDTable("user_roles") {
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val roleId = uuid("role_id").references(Roles.id, onDelete = ReferenceOption.CASCADE)
    
    // Denormalized for efficient queries (matches role's businessId)
    val businessId = uuid("business_id").references(Businesses.id, onDelete = ReferenceOption.CASCADE)
    
    // Who granted this role and when
    val grantedBy = uuid("granted_by").references(Users.id)
    val grantedAt = timestamp("granted_at")
    
    // Optional expiration for temporary access
    val expiresAt = timestamp("expires_at").nullable()
    
    // Revocation tracking (soft delete pattern)
    val revokedAt = timestamp("revoked_at").nullable()
    val revokedBy = uuid("revoked_by").references(Users.id).nullable()
    
    init {
        // A user can only have a specific role once (per business implied by role)
        uniqueIndex("unique_user_role", userId, roleId)
        
        // Index for "what roles does user X have" queries
        index("idx_user_roles_user", false, userId)
        
        // Index for "what roles does user X have in business Y" queries
        index("idx_user_roles_user_business", false, userId, businessId)
    }
}

