package com.ground.service

import com.ground.model.AuditLogs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

/**
 * AuditService - Logs all security and permission-related actions.
 * 
 * Every action that affects access control is logged here for:
 * - Security auditing
 * - Compliance requirements  
 * - Debugging access issues
 * - User activity tracking
 */
class AuditService {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    
    /**
     * Log an audit event
     */
    fun log(
        actorUserId: UUID,
        action: String,
        targetType: String,
        targetId: UUID? = null,
        businessId: UUID? = null,
        resourceType: String? = null,
        resourceId: UUID? = null,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): UUID {
        return transaction {
            AuditLogs.insert {
                it[AuditLogs.actorUserId] = actorUserId
                it[AuditLogs.actorIpAddress] = ipAddress
                it[AuditLogs.actorUserAgent] = userAgent
                it[AuditLogs.action] = action
                it[AuditLogs.targetType] = targetType
                it[AuditLogs.targetId] = targetId
                it[AuditLogs.businessId] = businessId
                it[AuditLogs.resourceType] = resourceType
                it[AuditLogs.resourceId] = resourceId
                it[AuditLogs.metadata] = metadata?.filterValues { it != null }?.let { json.encodeToString(it) }
                it[AuditLogs.createdAt] = Instant.now()
            } get AuditLogs.id
        }.value
    }
    
    /**
     * Log a user action (login, logout, password change, etc.)
     */
    fun logUserAction(
        actorUserId: UUID,
        action: String,
        targetUserId: UUID? = null,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ) = log(
        actorUserId = actorUserId,
        action = action,
        targetType = "user",
        targetId = targetUserId ?: actorUserId,
        metadata = metadata,
        ipAddress = ipAddress,
        userAgent = userAgent
    )
    
    /**
     * Log a business action (create, update, delete)
     */
    fun logBusinessAction(
        actorUserId: UUID,
        action: String,
        businessId: UUID,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null
    ) = log(
        actorUserId = actorUserId,
        action = action,
        targetType = "business",
        targetId = businessId,
        businessId = businessId,
        metadata = metadata,
        ipAddress = ipAddress
    )
    
    /**
     * Log a membership action (invite, accept, revoke)
     */
    fun logMembershipAction(
        actorUserId: UUID,
        action: String,
        businessId: UUID,
        targetUserId: UUID,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null
    ) = log(
        actorUserId = actorUserId,
        action = action,
        targetType = "membership",
        targetId = targetUserId,
        businessId = businessId,
        metadata = metadata,
        ipAddress = ipAddress
    )
    
    /**
     * Log a role action (create, update, delete, assign, revoke)
     */
    fun logRoleAction(
        actorUserId: UUID,
        action: String,
        roleId: UUID,
        businessId: UUID,
        targetUserId: UUID? = null,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null
    ) = log(
        actorUserId = actorUserId,
        action = action,
        targetType = "role",
        targetId = roleId,
        businessId = businessId,
        metadata = metadata?.plus("target_user_id" to targetUserId?.toString()),
        ipAddress = ipAddress
    )
    
    /**
     * Log a resource access action (grant, revoke, update)
     */
    fun logResourceAccessAction(
        actorUserId: UUID,
        action: String,
        resourceType: String,
        resourceId: UUID,
        accessId: UUID? = null,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null
    ) = log(
        actorUserId = actorUserId,
        action = action,
        targetType = "resource_access",
        targetId = accessId,
        resourceType = resourceType,
        resourceId = resourceId,
        metadata = metadata,
        ipAddress = ipAddress
    )
    
    /**
     * Log a resource action (create, update, delete)
     */
    fun logResourceAction(
        actorUserId: UUID,
        action: String,
        resourceType: String,
        resourceId: UUID,
        metadata: Map<String, String?>? = null,
        ipAddress: String? = null
    ) = log(
        actorUserId = actorUserId,
        action = action,
        targetType = resourceType,
        targetId = resourceId,
        resourceType = resourceType,
        resourceId = resourceId,
        metadata = metadata,
        ipAddress = ipAddress
    )
    
    /**
     * Query audit logs for a specific business
     */
    fun getBusinessAuditLogs(
        businessId: UUID,
        limit: Int = 100,
        offset: Int = 0
    ): List<AuditLogEntry> {
        return transaction {
            AuditLogs.select { AuditLogs.businessId eq businessId }
                .orderBy(AuditLogs.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { it.toAuditLogEntry() }
        }
    }
    
    /**
     * Query audit logs for a specific resource
     */
    fun getResourceAuditLogs(
        resourceType: String,
        resourceId: UUID,
        limit: Int = 100,
        offset: Int = 0
    ): List<AuditLogEntry> {
        return transaction {
            AuditLogs.select { 
                (AuditLogs.resourceType eq resourceType) and 
                (AuditLogs.resourceId eq resourceId)
            }
            .orderBy(AuditLogs.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { it.toAuditLogEntry() }
        }
    }
    
    /**
     * Query audit logs for a specific user (as actor)
     */
    fun getUserAuditLogs(
        userId: UUID,
        limit: Int = 100,
        offset: Int = 0
    ): List<AuditLogEntry> {
        return transaction {
            AuditLogs.select { AuditLogs.actorUserId eq userId }
                .orderBy(AuditLogs.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
                .limit(limit, offset.toLong())
                .map { it.toAuditLogEntry() }
        }
    }
    
    private fun org.jetbrains.exposed.sql.ResultRow.toAuditLogEntry() = AuditLogEntry(
        id = this[AuditLogs.id].value,
        actorUserId = this[AuditLogs.actorUserId],
        actorIpAddress = this[AuditLogs.actorIpAddress],
        action = this[AuditLogs.action],
        targetType = this[AuditLogs.targetType],
        targetId = this[AuditLogs.targetId],
        businessId = this[AuditLogs.businessId],
        resourceType = this[AuditLogs.resourceType],
        resourceId = this[AuditLogs.resourceId],
        metadata = this[AuditLogs.metadata],
        createdAt = this[AuditLogs.createdAt]
    )
}

/**
 * Data class for audit log entries
 */
data class AuditLogEntry(
    val id: UUID,
    val actorUserId: UUID,
    val actorIpAddress: String?,
    val action: String,
    val targetType: String,
    val targetId: UUID?,
    val businessId: UUID?,
    val resourceType: String?,
    val resourceId: UUID?,
    val metadata: String?,
    val createdAt: Instant
)

