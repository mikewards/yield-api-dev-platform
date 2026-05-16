package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * BusinessMemberships - Links users to businesses they can access.
 * 
 * A user can be a member of multiple businesses (many-to-many).
 * This table tracks:
 * - Who invited them
 * - When they accepted
 * - Current membership status
 * 
 * Status values:
 * - "pending": Invitation sent, not yet accepted
 * - "active": User has accepted and has access
 * - "revoked": Access has been revoked
 */
object BusinessMemberships : UUIDTable("business_memberships") {
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val businessId = uuid("business_id").references(Businesses.id, onDelete = ReferenceOption.CASCADE)
    
    // Invitation tracking
    val invitedBy = uuid("invited_by").references(Users.id).nullable() // null if owner/self-created
    val invitedAt = timestamp("invited_at").nullable()
    val invitationToken = varchar("invitation_token", 255).nullable().uniqueIndex()
    val invitationExpiresAt = timestamp("invitation_expires_at").nullable()
    val invitationEmail = varchar("invitation_email", 255).nullable() // Email invite was sent to
    
    // Acceptance
    val acceptedAt = timestamp("accepted_at").nullable()
    val status = varchar("status", 20).default("pending") // pending, active, revoked
    
    // Revocation tracking
    val revokedAt = timestamp("revoked_at").nullable()
    val revokedBy = uuid("revoked_by").references(Users.id).nullable()
    
    // Timestamps
    val createdAt = timestamp("created_at").nullable()
    
    init {
        // A user can only have one membership per business
        uniqueIndex("unique_user_business", userId, businessId)
    }
}

