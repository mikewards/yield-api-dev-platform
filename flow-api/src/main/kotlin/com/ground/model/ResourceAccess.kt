package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * ResourceAccess - THE CORE OF RCAC (Resource-Centric Access Control)
 * 
 * This table defines WHO can access WHAT resources with WHAT permission level.
 * 
 * Key principles:
 * 1. Resources are autonomous - they don't belong to businesses
 * 2. Access is explicit - every access requires a record here
 * 3. Access can be direct (to a user) OR via role (to all users with that role)
 * 4. Permissions are hierarchical: read < write < admin
 * 
 * Resource types:
 * - "application": Applications table
 * - "yield_account": YieldAccounts table
 * - "transaction": Transactions table
 * - "webhook": Webhooks table
 * - "api_key": ApiKeys table
 * - "wallet": ApplicationWallets table
 * 
 * Permission levels:
 * - "read": Can view the resource
 * - "write": Can view and modify the resource
 * - "admin": Can view, modify, delete, and share the resource
 * 
 * IMPORTANT: Either userId OR roleId must be set, never both, never neither.
 */
object ResourceAccess : UUIDTable("resource_access") {
    // ─────────────────────────────────────────────────────────
    // WHAT resource is being accessed
    // ─────────────────────────────────────────────────────────
    val resourceType = varchar("resource_type", 50)
    val resourceId = uuid("resource_id")
    
    // ─────────────────────────────────────────────────────────
    // WHO has access (exactly ONE of these must be set)
    // ─────────────────────────────────────────────────────────
    
    // Direct grant to a specific user
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE).nullable()
    
    // Grant to all users with this role
    val roleId = uuid("role_id").references(Roles.id, onDelete = ReferenceOption.CASCADE).nullable()
    
    // ─────────────────────────────────────────────────────────
    // WHAT level of access
    // ─────────────────────────────────────────────────────────
    val permission = varchar("permission", 20) // "read", "write", "admin"
    
    // ─────────────────────────────────────────────────────────
    // Additional scoping (optional)
    // ─────────────────────────────────────────────────────────
    val scope = varchar("scope", 50).nullable() // "sandbox_only", "production_only", etc.
    
    // ─────────────────────────────────────────────────────────
    // Provenance & audit
    // ─────────────────────────────────────────────────────────
    val grantedBy = uuid("granted_by").references(Users.id)
    val grantedAt = timestamp("granted_at")
    val grantReason = text("grant_reason").nullable() // Why was access granted
    
    // ─────────────────────────────────────────────────────────
    // Expiration (for temporary access)
    // ─────────────────────────────────────────────────────────
    val expiresAt = timestamp("expires_at").nullable()
    
    // ─────────────────────────────────────────────────────────
    // Revocation tracking (soft delete)
    // ─────────────────────────────────────────────────────────
    val revokedAt = timestamp("revoked_at").nullable()
    val revokedBy = uuid("revoked_by").references(Users.id).nullable()
    val revokeReason = text("revoke_reason").nullable()
    
    init {
        // Unique access per user per resource (nullable-safe)
        // Note: We'll enforce the constraint that exactly one of userId/roleId is set in application code
        index("idx_resource_access_user", false, resourceType, resourceId, userId)
        index("idx_resource_access_role", false, resourceType, resourceId, roleId)
        
        // Index for "what can user X access" queries
        index("idx_resource_access_by_user", false, userId)
        
        // Index for "what can role Y access" queries  
        index("idx_resource_access_by_role", false, roleId)
        
        // Index for "who can access resource Z" queries
        index("idx_resource_access_by_resource", false, resourceType, resourceId)
    }
}

