package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Users - Individual people who access the Ground platform.
 * 
 * This replaces the conflated Account model where Account = User + Business.
 * Now: User = Person, Business = Organization (separate entities).
 * 
 * Users can be members of multiple businesses with different roles.
 * 
 * Status values:
 * - "active": Normal user
 * - "suspended": Temporarily disabled
 * - "deleted": Soft deleted, scheduled for purge
 */
object Users : UUIDTable("users") {
    // Identity
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    
    // Authentication
    val emailVerifiedAt = timestamp("email_verified_at").nullable()
    val mfaEnabled = bool("mfa_enabled").default(false)
    val mfaSecret = varchar("mfa_secret", 255).nullable()
    
    // Security: Account lockout after failed login attempts
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
    val lastLoginAt = timestamp("last_login_at").nullable()
    val lastLoginIp = varchar("last_login_ip", 45).nullable()
    
    // Status
    val status = varchar("status", 20).default("active")
    
    // Soft delete: 30-day retention before hard delete
    val deletedAt = timestamp("deleted_at").nullable()
    val scheduledPurgeAt = timestamp("scheduled_purge_at").nullable()
    
    // Timestamps
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}

