package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Roles - Named permission groups within a business.
 * 
 * Each business has its own set of roles:
 * - System roles (owner, admin, developer, viewer) are auto-created and cannot be deleted
 * - Custom roles can be created by business admins
 * 
 * Roles are used to grant access to resources via ResourceAccess.
 * When a role is granted access to a resource, all users with that role get access.
 * 
 * System roles:
 * - "owner": Full access, cannot be removed from business owner
 * - "admin": Can manage team and roles, admin access to resources
 * - "developer": Can create/modify resources
 * - "analyst": Read-only access for reporting
 * - "viewer": Basic read-only access
 */
object Roles : UUIDTable("roles") {
    val businessId = uuid("business_id").references(Businesses.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100) // Internal name: "owner", "admin", "developer", "custom_role"
    val displayName = varchar("display_name", 100) // Display name: "Owner", "Admin", "Developer"
    val description = text("description").nullable()
    val color = varchar("color", 7).nullable() // Hex color for UI badges: "#6366f1"
    
    // System roles cannot be modified or deleted
    val isSystem = bool("is_system").default(false)
    
    // Default role is auto-assigned to new members (optional, one per business)
    val isDefault = bool("is_default").default(false)
    
    // Business-level permissions this role grants (JSON array)
    // e.g., ["members:view", "members:invite", "roles:view"]
    val businessPermissions = text("business_permissions").nullable()
    
    // Who created this role
    val createdBy = uuid("created_by").references(Users.id)
    
    // Timestamps
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
    
    init {
        // Role names must be unique within a business
        uniqueIndex("unique_business_role", businessId, name)
    }
}

