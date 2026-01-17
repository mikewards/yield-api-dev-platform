package com.ground.database

import com.ground.model.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Schema {
    fun createTables() {
        transaction {
            try {
                SchemaUtils.createMissingTablesAndColumns(
                    // ═══════════════════════════════════════════════════════════
                    // RCAC (Resource-Centric Access Control) Tables
                    // Order matters due to foreign key dependencies!
                    // ═══════════════════════════════════════════════════════════
                    
                    // 1. Users - must be first (no dependencies)
                    Users,
                    
                    // 2. Businesses - depends on Users (ownerId)
                    Businesses,
                    
                    // 3. BusinessMemberships - depends on Users, Businesses
                    BusinessMemberships,
                    
                    // 4. Roles - depends on Businesses, Users
                    Roles,
                    
                    // 5. UserRoles - depends on Users, Roles, Businesses
                    UserRoles,
                    
                    // 6. ResourceAccess - depends on Users, Roles
                    ResourceAccess,
                    
                    // 7. AuditLogs - depends on Users, Businesses
                    AuditLogs,
                    
                    // ═══════════════════════════════════════════════════════════
                    // Legacy Tables (kept for migration, will be deprecated)
                    // ═══════════════════════════════════════════════════════════
                    Accounts,
                    
                    // ═══════════════════════════════════════════════════════════
                    // Resource Tables (autonomous entities)
                    // ═══════════════════════════════════════════════════════════
                    Applications,
                    ApplicationWallets,
                    YieldAccounts,
                    Positions,
                    Transactions,
                    Webhooks,
                    
                    // ═══════════════════════════════════════════════════════════
                    // Auth & Session Tables
                    // ═══════════════════════════════════════════════════════════
                    AccessTokens,
                    RefreshTokens,
                    UserSessions,
                    
                    // ═══════════════════════════════════════════════════════════
                    // Operational Tables
                    // ═══════════════════════════════════════════════════════════
                    RequestLogs
                )
            } catch (e: Exception) {
                // If createMissingTablesAndColumns fails, try create (for new databases)
                println("Creating tables with create()...")
                SchemaUtils.create(
                    // RCAC Tables (in dependency order)
                    Users,
                    Businesses,
                    BusinessMemberships,
                    Roles,
                    UserRoles,
                    ResourceAccess,
                    AuditLogs,
                    
                    // Legacy Tables
                    Accounts,
                    
                    // Resource Tables
                    Applications,
                    ApplicationWallets,
                    YieldAccounts,
                    Positions,
                    Transactions,
                    Webhooks,
                    
                    // Auth & Session Tables
                    AccessTokens,
                    RefreshTokens,
                    UserSessions,
                    
                    // Operational Tables
                    RequestLogs
                )
            }
        }
    }
}
