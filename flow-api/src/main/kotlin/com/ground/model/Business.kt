package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Businesses - Organizations that group users and define roles.
 * 
 * IMPORTANT: Businesses do NOT own resources!
 * Resources are autonomous entities with explicit access grants.
 * Businesses are containers for:
 * - Grouping users (via BusinessMemberships)
 * - Defining roles (via Roles)
 * - Billing context
 * 
 * The business owner (ownerId) always retains full access and cannot be removed.
 * 
 * Status values:
 * - "active": Normal business
 * - "suspended": Temporarily disabled (e.g., billing issue)
 * - "deleted": Soft deleted
 */
object Businesses : UUIDTable("businesses") {
    // Identity
    val name = varchar("name", 255)
    val slug = varchar("slug", 100).uniqueIndex() // URL-friendly: "acme-corp"
    val logoUrl = varchar("logo_url", 500).nullable()
    val website = varchar("website", 500).nullable()
    val description = text("description").nullable()
    
    // Plan & Billing
    val plan = varchar("plan", 50).default("free") // free, starter, pro, enterprise
    val billingEmail = varchar("billing_email", 255).nullable()
    val stripeCustomerId = varchar("stripe_customer_id", 255).nullable()
    
    // The user who created this business (always retains owner access)
    val ownerId = uuid("owner_id").references(Users.id)
    
    // Status
    val status = varchar("status", 20).default("active")
    
    // Timestamps
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}

