package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * AuditLogs - Complete history of all security and permission changes.
 * 
 * Every permission-related action is logged here for:
 * - Security auditing
 * - Compliance requirements
 * - Debugging access issues
 * - User activity tracking
 * 
 * Action categories:
 * 
 * User actions:
 * - user_created, user_updated, user_deleted
 * - login_success, login_failed, logout
 * - mfa_enabled, mfa_disabled
 * - password_changed, password_reset
 * 
 * Business actions:
 * - business_created, business_updated, business_deleted
 * - membership_invited, membership_accepted, membership_declined, membership_revoked
 * 
 * Role actions:
 * - role_created, role_updated, role_deleted
 * - role_assigned, role_revoked
 * 
 * Resource actions:
 * - resource_created, resource_updated, resource_deleted
 * - access_granted, access_updated, access_revoked
 * 
 * API Key actions:
 * - api_key_created, api_key_used, api_key_revoked
 */
object AuditLogs : UUIDTable("audit_logs") {
    // Who performed the action
    val actorUserId = uuid("actor_user_id").references(Users.id)
    val actorIpAddress = varchar("actor_ip_address", 45).nullable() // IPv6 can be up to 45 chars
    val actorUserAgent = text("actor_user_agent").nullable()
    
    // What action was performed
    val action = varchar("action", 50)
    
    // What was affected
    val targetType = varchar("target_type", 50) // "user", "business", "role", "resource_access", etc.
    val targetId = uuid("target_id").nullable()
    
    // Business context (if applicable)
    val businessId = uuid("business_id").references(Businesses.id).nullable()
    
    // Resource context (if applicable)
    val resourceType = varchar("resource_type", 50).nullable()
    val resourceId = uuid("resource_id").nullable()
    
    // Additional context as JSON
    // Example: {"old_permission": "read", "new_permission": "write", "reason": "Promoted to lead"}
    val metadata = text("metadata").nullable()
    
    // Timestamp (immutable - audit logs are never updated)
    val createdAt = timestamp("created_at")
    
    init {
        // Index for querying by actor
        index("idx_audit_actor", false, actorUserId)
        
        // Index for querying by business
        index("idx_audit_business", false, businessId)
        
        // Index for querying by target
        index("idx_audit_target", false, targetType, targetId)
        
        // Index for querying by resource
        index("idx_audit_resource", false, resourceType, resourceId)
        
        // Index for querying by time range
        index("idx_audit_time", false, createdAt)
        
        // Composite index for common query: "audit logs for business X in time range"
        index("idx_audit_business_time", false, businessId, createdAt)
    }
}

