package com.tbd.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * User accounts table with security features:
 * - BCrypt password hashing
 * - Account lockout after failed login attempts
 * - Soft delete with 30-day retention
 * 
 * Status values:
 * - "active": Normal account
 * - "deleted": Soft deleted, scheduled for purge
 */
object Accounts : UUIDTable("accounts") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val email = varchar("email", 255).nullable()
    val status = varchar("status", 20).default("active")
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
    
    // Security: Account lockout after failed login attempts
    val failedLoginAttempts = integer("failed_login_attempts").default(0)
    val lockedUntil = timestamp("locked_until").nullable()
    
    // Soft delete: 30-day retention before hard delete
    val deletedAt = timestamp("deleted_at").nullable()
    val scheduledPurgeAt = timestamp("scheduled_purge_at").nullable()
}
