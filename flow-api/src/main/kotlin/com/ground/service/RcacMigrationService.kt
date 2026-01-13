package com.ground.service

import com.ground.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * Migration service to convert legacy Account-based data to RCAC model.
 * 
 * This migration:
 * 1. Creates a User for each Account (same credentials)
 * 2. Creates a Business for each Account
 * 3. Creates BusinessMembership (user as owner)
 * 4. Creates system Roles for each business
 * 5. Assigns Owner role to the user
 * 6. Creates ResourceAccess records for all existing resources
 * 7. Updates resources to set createdBy field
 * 
 * The migration is idempotent - can be run multiple times safely.
 */
class RcacMigrationService(
    private val roleService: RoleService = RoleService(),
    private val auditService: AuditService = AuditService()
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    data class MigrationResult(
        val accountsMigrated: Int,
        val usersMigrated: Int,
        val businessesCreated: Int,
        val resourceAccessRecordsCreated: Int,
        val errors: List<String>
    )
    
    /**
     * Run the full migration.
     * This is idempotent - can be run multiple times.
     */
    fun migrate(): MigrationResult {
        val errors = mutableListOf<String>()
        var accountsMigrated = 0
        var usersMigrated = 0
        var businessesCreated = 0
        var resourceAccessRecordsCreated = 0
        
        println("═══════════════════════════════════════════════════════════")
        println("  RCAC MIGRATION - Starting")
        println("═══════════════════════════════════════════════════════════")
        
        transaction {
            // Get all accounts
            val accounts = Accounts.selectAll().toList()
            println("Found ${accounts.size} accounts to migrate")
            
            for (account in accounts) {
                try {
                    val accountId = account[Accounts.id].value
                    val username = account[Accounts.username]
                    val email = account[Accounts.email] ?: "$username@migrated.ground.dev"
                    val passwordHash = account[Accounts.passwordHash]
                    val status = account[Accounts.status]
                    val createdAt = account[Accounts.createdAt] ?: Instant.now()
                    
                    println("\n─────────────────────────────────────────────────────────")
                    println("Migrating account: $username (ID: $accountId)")
                    
                    // Check if already migrated (user with same email exists)
                    val existingUser = Users.select { Users.email eq email.lowercase() }.firstOrNull()
                    
                    val userId: UUID
                    val businessId: UUID
                    
                    if (existingUser != null) {
                        userId = existingUser[Users.id].value
                        println("  → User already exists: $userId")
                        
                        // Check if business exists for this user
                        val existingBusiness = Businesses.select { Businesses.ownerId eq userId }.firstOrNull()
                        if (existingBusiness != null) {
                            businessId = existingBusiness[Businesses.id].value
                            println("  → Business already exists: $businessId")
                        } else {
                            // Create business
                            businessId = createBusinessForUser(userId, username, createdAt)
                            businessesCreated++
                            println("  → Created business: $businessId")
                        }
                    } else {
                        // Create new user
                        userId = Users.insertAndGetId {
                            it[Users.email] = email.lowercase()
                            it[Users.passwordHash] = passwordHash
                            it[firstName] = username
                            it[Users.status] = if (status == "active") "active" else status
                            it[Users.createdAt] = createdAt
                            it[updatedAt] = Instant.now()
                            it[failedLoginAttempts] = account[Accounts.failedLoginAttempts]
                            it[lockedUntil] = account[Accounts.lockedUntil]
                            it[deletedAt] = account[Accounts.deletedAt]
                            it[scheduledPurgeAt] = account[Accounts.scheduledPurgeAt]
                        }.value
                        usersMigrated++
                        println("  → Created user: $userId")
                        
                        // Create business
                        businessId = createBusinessForUser(userId, username, createdAt)
                        businessesCreated++
                        println("  → Created business: $businessId")
                    }
                    
                    // Store mapping in a migration tracking table (using metadata in AuditLogs)
                    // This helps with debugging and rollback if needed
                    
                    // Migrate resources owned by this account
                    val accessCreated = migrateAccountResources(accountId, userId, businessId)
                    resourceAccessRecordsCreated += accessCreated
                    println("  → Created $accessCreated resource access records")
                    
                    accountsMigrated++
                    
                } catch (e: Exception) {
                    val errorMsg = "Error migrating account ${account[Accounts.username]}: ${e.message}"
                    errors.add(errorMsg)
                    println("  ✗ ERROR: $errorMsg")
                    e.printStackTrace()
                }
            }
        }
        
        println("\n═══════════════════════════════════════════════════════════")
        println("  RCAC MIGRATION - Complete")
        println("═══════════════════════════════════════════════════════════")
        println("  Accounts processed: $accountsMigrated")
        println("  Users created: $usersMigrated")
        println("  Businesses created: $businessesCreated")
        println("  Resource access records: $resourceAccessRecordsCreated")
        println("  Errors: ${errors.size}")
        if (errors.isNotEmpty()) {
            println("\n  Errors:")
            errors.forEach { println("    - $it") }
        }
        println("═══════════════════════════════════════════════════════════\n")
        
        return MigrationResult(
            accountsMigrated = accountsMigrated,
            usersMigrated = usersMigrated,
            businessesCreated = businessesCreated,
            resourceAccessRecordsCreated = resourceAccessRecordsCreated,
            errors = errors
        )
    }
    
    /**
     * Create a business for a user.
     */
    private fun createBusinessForUser(userId: UUID, username: String, createdAt: Instant): UUID {
        val now = Instant.now()
        val slug = generateSlug(username)
        
        val businessId = Businesses.insertAndGetId {
            it[name] = "$username's Business"
            it[Businesses.slug] = slug
            it[ownerId] = userId
            it[status] = "active"
            it[plan] = "free"
            it[Businesses.createdAt] = createdAt
            it[updatedAt] = now
        }.value
        
        // Create membership (owner)
        BusinessMemberships.insert {
            it[BusinessMemberships.userId] = userId
            it[BusinessMemberships.businessId] = businessId
            it[status] = "active"
            it[acceptedAt] = now
            it[BusinessMemberships.createdAt] = now
        }
        
        // Create system roles
        val ownerRoleId = createSystemRolesForBusiness(businessId, userId)
        
        // Assign owner role
        UserRoles.insert {
            it[UserRoles.userId] = userId
            it[roleId] = ownerRoleId
            it[UserRoles.businessId] = businessId
            it[grantedBy] = userId
            it[grantedAt] = now
        }
        
        return businessId
    }
    
    /**
     * Create system roles for a business.
     * Returns the owner role ID.
     */
    private fun createSystemRolesForBusiness(businessId: UUID, createdBy: UUID): UUID {
        val now = Instant.now()
        var ownerRoleId: UUID? = null
        
        val systemRoles = listOf(
            Triple("owner", "Owner", "#dc2626"),
            Triple("admin", "Admin", "#7c3aed"),
            Triple("developer", "Developer", "#2563eb"),
            Triple("analyst", "Analyst", "#059669"),
            Triple("viewer", "Viewer", "#6b7280")
        )
        
        for ((name, displayName, color) in systemRoles) {
            // Check if role already exists
            val existing = Roles.select { 
                (Roles.businessId eq businessId) and (Roles.name eq name) 
            }.firstOrNull()
            
            if (existing != null) {
                if (name == "owner") ownerRoleId = existing[Roles.id].value
                continue
            }
            
            val permissions = when (name) {
                "owner" -> listOf("*")
                "admin" -> listOf("members:*", "roles:*", "settings:*", "audit:view")
                "developer" -> listOf("members:view", "roles:view")
                else -> listOf("members:view")
            }
            
            val roleId = Roles.insertAndGetId {
                it[Roles.businessId] = businessId
                it[Roles.name] = name
                it[Roles.displayName] = displayName
                it[description] = getSystemRoleDescription(name)
                it[Roles.color] = color
                it[isSystem] = true
                it[isDefault] = name == "viewer"
                it[businessPermissions] = json.encodeToString(permissions)
                it[Roles.createdBy] = createdBy
                it[createdAt] = now
                it[updatedAt] = now
            }.value
            
            if (name == "owner") ownerRoleId = roleId
        }
        
        return ownerRoleId ?: throw IllegalStateException("Failed to create owner role")
    }
    
    private fun getSystemRoleDescription(name: String): String = when (name) {
        "owner" -> "Full access to all business settings and resources. Cannot be removed."
        "admin" -> "Can manage team members, roles, and business settings."
        "developer" -> "Can create and manage applications and API resources."
        "analyst" -> "Read-only access for analytics and reporting."
        "viewer" -> "Basic read-only access."
        else -> ""
    }
    
    /**
     * Migrate all resources owned by an account.
     * Returns the number of ResourceAccess records created.
     */
    private fun migrateAccountResources(accountId: UUID, userId: UUID, businessId: UUID): Int {
        val now = Instant.now()
        var count = 0
        
        // Get the owner role for this business
        val ownerRole = Roles.select { 
            (Roles.businessId eq businessId) and (Roles.name eq "owner") 
        }.firstOrNull()
        val ownerRoleId = ownerRole?.get(Roles.id)?.value
        
        // ═══════════════════════════════════════════════════════════
        // APPLICATIONS
        // ═══════════════════════════════════════════════════════════
        Applications.select { Applications.accountId eq accountId }.forEach { app ->
            val appId = app[Applications.id].value
            
            // Update createdBy if null
            if (app[Applications.createdBy] == null) {
                Applications.update({ Applications.id eq appId }) {
                    it[createdBy] = userId
                }
            }
            
            // Create ResourceAccess if not exists
            val existingAccess = ResourceAccess.select {
                (ResourceAccess.resourceType eq "application") and
                (ResourceAccess.resourceId eq appId) and
                (ResourceAccess.userId eq userId)
            }.firstOrNull()
            
            if (existingAccess == null) {
                ResourceAccess.insert {
                    it[resourceType] = "application"
                    it[resourceId] = appId
                    it[ResourceAccess.userId] = userId
                    it[permission] = "admin"
                    it[grantedBy] = userId
                    it[grantedAt] = now
                    it[grantReason] = "Migrated from account ownership"
                }
                count++
            }
            
            // Also grant to owner role if exists
            if (ownerRoleId != null) {
                val existingRoleAccess = ResourceAccess.select {
                    (ResourceAccess.resourceType eq "application") and
                    (ResourceAccess.resourceId eq appId) and
                    (ResourceAccess.roleId eq ownerRoleId)
                }.firstOrNull()
                
                if (existingRoleAccess == null) {
                    ResourceAccess.insert {
                        it[resourceType] = "application"
                        it[resourceId] = appId
                        it[roleId] = ownerRoleId
                        it[permission] = "admin"
                        it[grantedBy] = userId
                        it[grantedAt] = now
                        it[grantReason] = "Migrated from account ownership (via owner role)"
                    }
                    count++
                }
            }
        }
        
        // ═══════════════════════════════════════════════════════════
        // YIELD ACCOUNTS
        // ═══════════════════════════════════════════════════════════
        YieldAccounts.select { YieldAccounts.accountId eq accountId }.forEach { ya ->
            val yaId = ya[YieldAccounts.id].value
            
            // Update createdBy if null
            if (ya[YieldAccounts.createdBy] == null) {
                YieldAccounts.update({ YieldAccounts.id eq yaId }) {
                    it[createdBy] = userId
                }
            }
            
            // Create ResourceAccess if not exists
            val existingAccess = ResourceAccess.select {
                (ResourceAccess.resourceType eq "yield_account") and
                (ResourceAccess.resourceId eq yaId) and
                (ResourceAccess.userId eq userId)
            }.firstOrNull()
            
            if (existingAccess == null) {
                ResourceAccess.insert {
                    it[resourceType] = "yield_account"
                    it[resourceId] = yaId
                    it[ResourceAccess.userId] = userId
                    it[permission] = "admin"
                    it[grantedBy] = userId
                    it[grantedAt] = now
                    it[grantReason] = "Migrated from account ownership"
                }
                count++
            }
        }
        
        // ═══════════════════════════════════════════════════════════
        // WEBHOOKS
        // ═══════════════════════════════════════════════════════════
        Webhooks.select { Webhooks.accountId eq accountId }.forEach { wh ->
            val whId = wh[Webhooks.id].value
            
            // Update createdBy if null
            if (wh[Webhooks.createdBy] == null) {
                Webhooks.update({ Webhooks.id eq whId }) {
                    it[createdBy] = userId
                }
            }
            
            // Create ResourceAccess if not exists
            val existingAccess = ResourceAccess.select {
                (ResourceAccess.resourceType eq "webhook") and
                (ResourceAccess.resourceId eq whId) and
                (ResourceAccess.userId eq userId)
            }.firstOrNull()
            
            if (existingAccess == null) {
                ResourceAccess.insert {
                    it[resourceType] = "webhook"
                    it[resourceId] = whId
                    it[ResourceAccess.userId] = userId
                    it[permission] = "admin"
                    it[grantedBy] = userId
                    it[grantedAt] = now
                    it[grantReason] = "Migrated from account ownership"
                }
                count++
            }
        }
        
        // ═══════════════════════════════════════════════════════════
        // TRANSACTIONS (read-only access for historical transactions)
        // ═══════════════════════════════════════════════════════════
        Transactions.select { Transactions.accountId eq accountId }.forEach { tx ->
            val txId = tx[Transactions.id].value
            
            // Update createdBy if null
            if (tx[Transactions.createdBy] == null) {
                Transactions.update({ Transactions.id eq txId }) {
                    it[createdBy] = userId
                }
            }
            
            // For transactions, we typically don't need individual ResourceAccess
            // They're accessed via their parent YieldAccount
        }
        
        return count
    }
    
    private fun generateSlug(name: String): String {
        val base = name.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .take(50)
        
        // Check if slug exists and add suffix if needed
        var slug = base
        var counter = 1
        while (Businesses.select { Businesses.slug eq slug }.firstOrNull() != null) {
            slug = "$base-$counter"
            counter++
        }
        return slug
    }
    
    /**
     * Get migration status.
     */
    fun getMigrationStatus(): Map<String, Any> {
        return transaction {
            val totalAccounts = Accounts.selectAll().count()
            val totalUsers = Users.selectAll().count()
            val totalBusinesses = Businesses.selectAll().count()
            val totalResourceAccess = ResourceAccess.selectAll().count()
            
            // Count resources with createdBy set
            val appsWithCreatedBy = Applications.select { Applications.createdBy.isNotNull() }.count()
            val totalApps = Applications.selectAll().count()
            
            mapOf(
                "total_accounts" to totalAccounts,
                "total_users" to totalUsers,
                "total_businesses" to totalBusinesses,
                "total_resource_access_records" to totalResourceAccess,
                "applications_migrated" to "$appsWithCreatedBy / $totalApps",
                "migration_complete" to (totalAccounts == totalUsers)
            )
        }
    }
}

