package com.ground.service

import com.ground.dto.*
import com.ground.model.AccessTokens
import com.ground.model.Applications
import com.ground.model.ApplicationWallets
import com.ground.model.RequestLogs
import com.ground.model.ResourceAccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom
import java.time.Instant
import java.util.*

class ApplicationService {
    private val json = Json { ignoreUnknownKeys = true }
    private val walletService = WalletService()
    private val permissionService = PermissionService()
    
    // ═══════════════════════════════════════════════════════════════
    // RCAC-AWARE METHODS (New - use these for new code)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create an application with RCAC permission grant.
     * The creator automatically gets admin access.
     */
    fun createApplicationRcac(userId: UUID, request: CreateApplicationRequest): ApplicationResponse {
        val now = Instant.now()
        val webhookSecret = if (request.webhook_url != null) generateWebhookSecret() else null
        
        val applicationId = transaction {
            Applications.insert {
                it[Applications.createdBy] = userId
                it[Applications.accountId] = null // No legacy accountId
                it[Applications.name] = request.name
                it[Applications.description] = request.description
                it[Applications.environment] = request.environment ?: "both"
                it[Applications.status] = "active"
                it[Applications.webhookUrl] = request.webhook_url
                it[Applications.webhookSecret] = webhookSecret
                it[Applications.allowedOrigins] = request.allowed_origins?.let { origins -> json.encodeToString(origins) }
                it[Applications.permissions] = request.permissions?.let { perms -> json.encodeToString(perms) }
                it[Applications.sandboxRpcUrl] = null
                it[Applications.productionRpcUrl] = null
                it[Applications.createdAt] = now
                it[Applications.updatedAt] = now
            } get Applications.id
        }
        
        // Grant admin access to creator
        permissionService.grantCreatorAccess(
            creatorId = userId,
            resourceType = "application",
            resourceId = applicationId.value
        )
        
        // Create default wallet
        walletService.createWallet(
            applicationId = applicationId.value,
            environment = request.environment ?: "sandbox",
            chain = "ethereum",
            label = "Default Wallet"
        )
        
        return getApplicationById(applicationId.value) 
            ?: throw IllegalStateException("Application not found after creation")
    }
    
    /**
     * List applications the user has access to (via RCAC).
     */
    fun listApplicationsRcac(userId: UUID): List<ApplicationResponse> {
        val accessibleIds = permissionService.getAccessibleResourceIds(userId, "application", "read")
        
        return transaction {
            Applications.select { Applications.id inList accessibleIds }
                .map { it.toApplicationResponse() }
        }
    }
    
    /**
     * Get application by ID with RCAC permission check.
     */
    fun getApplicationRcac(userId: UUID, applicationId: UUID, requiredPermission: String = "read"): ApplicationResponse? {
        if (!permissionService.canAccessResource(userId, "application", applicationId, requiredPermission)) {
            return null
        }
        return getApplicationById(applicationId)
    }
    
    /**
     * Update application with RCAC permission check.
     */
    fun updateApplicationRcac(userId: UUID, applicationId: UUID, request: UpdateApplicationRequest): ApplicationResponse? {
        if (!permissionService.canAccessResource(userId, "application", applicationId, "write")) {
            return null
        }
        
        return transaction {
            val now = Instant.now()
            
            val updated = Applications.update({ Applications.id eq applicationId }) {
                request.name?.let { name -> it[Applications.name] = name }
                request.description?.let { desc -> it[Applications.description] = desc }
                request.webhook_url?.let { url -> 
                    it[Applications.webhookUrl] = url
                    if (url.isNotBlank()) {
                        it[Applications.webhookSecret] = generateWebhookSecret()
                    }
                }
                request.allowed_origins?.let { origins -> it[Applications.allowedOrigins] = json.encodeToString(origins) }
                request.permissions?.let { perms -> it[Applications.permissions] = json.encodeToString(perms) }
                request.status?.let { status -> it[Applications.status] = status }
                request.sandbox_rpc_url?.let { url -> it[Applications.sandboxRpcUrl] = url }
                request.production_rpc_url?.let { url -> it[Applications.productionRpcUrl] = url }
                it[Applications.updatedAt] = now
            }
            
            if (updated > 0) {
                getApplicationById(applicationId)
            } else {
                null
            }
        }
    }
    
    /**
     * Delete application with RCAC permission check.
     */
    fun deleteApplicationRcac(userId: UUID, applicationId: UUID): Boolean {
        if (!permissionService.canAccessResource(userId, "application", applicationId, "admin")) {
            return false
        }
        
        return transaction {
            // Delete wallets, tokens, request logs references
            ApplicationWallets.deleteWhere { ApplicationWallets.applicationId eq applicationId }
            AccessTokens.deleteWhere { AccessTokens.applicationId eq applicationId }
            RequestLogs.update({ RequestLogs.applicationId eq applicationId }) {
                it[RequestLogs.applicationId] = null
            }
            
            // Delete ResourceAccess records for this application
            ResourceAccess.deleteWhere { 
                (ResourceAccess.resourceType eq "application") and (ResourceAccess.resourceId eq applicationId)
            }
            
            // Delete the application
            val deleted = Applications.deleteWhere { Applications.id eq applicationId }
            deleted > 0
        }
    }
    
    /**
     * Get application by ID (internal, no permission check).
     */
    private fun getApplicationById(applicationId: UUID): ApplicationResponse? {
        return transaction {
            Applications.select { Applications.id eq applicationId }
                .firstOrNull()?.toApplicationResponse()
        }
    }
    
    private fun ResultRow.toApplicationResponse(): ApplicationResponse {
        return ApplicationResponse(
            application_id = this[Applications.id].value.toString(),
            name = this[Applications.name],
            description = this[Applications.description],
            environment = this[Applications.environment],
            status = this[Applications.status],
            webhook_url = this[Applications.webhookUrl],
            webhook_secret = this[Applications.webhookSecret],
            allowed_origins = this[Applications.allowedOrigins]?.let { json.decodeFromString<List<String>>(it) },
            permissions = this[Applications.permissions]?.let { json.decodeFromString<List<String>>(it) },
            sandbox_rpc_url = this[Applications.sandboxRpcUrl],
            production_rpc_url = this[Applications.productionRpcUrl],
            created_at = this[Applications.createdAt]?.toString() ?: "",
            updated_at = this[Applications.updatedAt]?.toString()
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LEGACY METHODS (Backward compatible - use accountId)
    // These will be deprecated after migration is complete
    // ═══════════════════════════════════════════════════════════════
    
    fun createApplication(accountId: UUID, request: CreateApplicationRequest): ApplicationResponse {
        val now = Instant.now()
        val webhookSecret = if (request.webhook_url != null) generateWebhookSecret() else null
        
        // Create application (RPC URLs not needed - we handle blockchain infrastructure internally)
        val applicationId = transaction {
            Applications.insert {
                it[Applications.accountId] = accountId
                it[Applications.name] = request.name
                it[Applications.description] = request.description
                it[Applications.environment] = request.environment ?: "both"
                it[Applications.status] = "active"
                it[Applications.webhookUrl] = request.webhook_url
                it[Applications.webhookSecret] = webhookSecret
                it[Applications.allowedOrigins] = request.allowed_origins?.let { origins -> json.encodeToString(origins) }
                it[Applications.permissions] = request.permissions?.let { perms -> json.encodeToString(perms) }
                // RPC URLs deprecated - not exposed to developers
                it[Applications.sandboxRpcUrl] = null
                it[Applications.productionRpcUrl] = null
                it[Applications.createdAt] = now
                it[Applications.updatedAt] = now
            } get Applications.id
        }
        
        // Automatically create a wallet for this application (default to sandbox)
        val wallet = walletService.createWallet(
            applicationId = applicationId.value,
            environment = request.environment ?: "sandbox",
            chain = "ethereum",
            label = "Default Wallet"
        )
        
        return transaction {
            Applications.select { Applications.id eq applicationId.value }
                .firstOrNull()?.let { row ->
                    ApplicationResponse(
                        application_id = row[Applications.id].value.toString(),
                        name = row[Applications.name],
                        description = row[Applications.description],
                        environment = row[Applications.environment],
                        status = row[Applications.status],
                        webhook_url = row[Applications.webhookUrl],
                        webhook_secret = row[Applications.webhookSecret],
                        allowed_origins = row[Applications.allowedOrigins]?.let { json.decodeFromString<List<String>>(it) },
                        permissions = row[Applications.permissions]?.let { json.decodeFromString<List<String>>(it) },
                        sandbox_rpc_url = row[Applications.sandboxRpcUrl],
                        production_rpc_url = row[Applications.productionRpcUrl],
                        created_at = row[Applications.createdAt]?.toString() ?: "",
                        updated_at = row[Applications.updatedAt]?.toString()
                    )
                } ?: throw IllegalStateException("Application not found after creation")
        }
    }
    
    fun listApplications(accountId: UUID): List<ApplicationResponse> {
        return transaction {
            Applications.select { Applications.accountId eq accountId }
                .map { row ->
                    ApplicationResponse(
                        application_id = row[Applications.id].value.toString(),
                        name = row[Applications.name],
                        description = row[Applications.description],
                        environment = row[Applications.environment],
                        status = row[Applications.status],
                        webhook_url = row[Applications.webhookUrl],
                        webhook_secret = row[Applications.webhookSecret],
                        allowed_origins = row[Applications.allowedOrigins]?.let { json.decodeFromString<List<String>>(it) },
                        permissions = row[Applications.permissions]?.let { json.decodeFromString<List<String>>(it) },
                        sandbox_rpc_url = row[Applications.sandboxRpcUrl],
                        production_rpc_url = row[Applications.productionRpcUrl],
                        created_at = row[Applications.createdAt]?.toString() ?: "",
                        updated_at = row[Applications.updatedAt]?.toString()
                    )
                }
        }
    }
    
    fun getApplication(accountId: UUID, applicationId: UUID): ApplicationResponse? {
        return transaction {
            Applications.select { 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }.firstOrNull()?.let { row ->
                ApplicationResponse(
                    application_id = row[Applications.id].value.toString(),
                    name = row[Applications.name],
                    description = row[Applications.description],
                    environment = row[Applications.environment],
                    status = row[Applications.status],
                    webhook_url = row[Applications.webhookUrl],
                    webhook_secret = row[Applications.webhookSecret],
                    allowed_origins = row[Applications.allowedOrigins]?.let { json.decodeFromString<List<String>>(it) },
                    permissions = row[Applications.permissions]?.let { json.decodeFromString<List<String>>(it) },
                    sandbox_rpc_url = row[Applications.sandboxRpcUrl],
                    production_rpc_url = row[Applications.productionRpcUrl],
                    created_at = row[Applications.createdAt]?.toString() ?: "",
                    updated_at = row[Applications.updatedAt]?.toString()
                )
            }
        }
    }
    
    fun updateApplication(accountId: UUID, applicationId: UUID, request: UpdateApplicationRequest): ApplicationResponse? {
        return transaction {
            val now = Instant.now()
            
            val updated = Applications.update({ 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }) {
                request.name?.let { name -> it[Applications.name] = name }
                request.description?.let { desc -> it[Applications.description] = desc }
                request.webhook_url?.let { url -> 
                    it[Applications.webhookUrl] = url
                    // Generate new webhook secret if URL is being set
                    if (url.isNotBlank()) {
                        it[Applications.webhookSecret] = generateWebhookSecret()
                    }
                }
                request.allowed_origins?.let { origins -> it[Applications.allowedOrigins] = json.encodeToString(origins) }
                request.permissions?.let { perms -> it[Applications.permissions] = json.encodeToString(perms) }
                request.status?.let { status -> it[Applications.status] = status }
                request.sandbox_rpc_url?.let { url -> it[Applications.sandboxRpcUrl] = url }
                request.production_rpc_url?.let { url -> it[Applications.productionRpcUrl] = url }
                it[Applications.updatedAt] = now
            }
            
            if (updated > 0) {
                getApplication(accountId, applicationId)
            } else {
                null
            }
        }
    }
    
    fun deleteApplication(accountId: UUID, applicationId: UUID): Boolean {
        return transaction {
            // First verify the application belongs to this account
            val app = Applications.select { 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }.firstOrNull() ?: return@transaction false
            
            // Delete all wallets associated with this application
            ApplicationWallets.deleteWhere { ApplicationWallets.applicationId eq applicationId }
            
            // Delete all tokens associated with this application
            AccessTokens.deleteWhere { AccessTokens.applicationId eq applicationId }
            
            // Set request logs applicationId to null (they reference this app)
            RequestLogs.update({ RequestLogs.applicationId eq applicationId }) {
                it[RequestLogs.applicationId] = null
            }
            
            // Finally delete the application
            val deleted = Applications.deleteWhere { 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }
            deleted > 0
        }
    }
    
    fun regenerateWebhookSecret(accountId: UUID, applicationId: UUID): String? {
        return transaction {
            val newSecret = generateWebhookSecret()
            val updated = Applications.update({
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }) {
                it[Applications.webhookSecret] = newSecret
                it[Applications.updatedAt] = Instant.now()
            }
            
            if (updated > 0) newSecret else null
        }
    }
    
    // Token management for applications
    fun createAppToken(accountId: UUID, applicationId: UUID, request: CreateAppTokenRequest): AppTokenResponse {
        val tokenService = TokenService()
        
        return transaction {
            // Verify application belongs to account
            val app = Applications.select { 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }.firstOrNull() ?: throw IllegalArgumentException("Application not found")
            
            val now = Instant.now()
            // Use environment from request, or default to sandbox
            val tokenEnvironment = request.environment ?: "sandbox"
            val token = "tbd_${tokenEnvironment.substring(0, 4)}_${UUID.randomUUID().toString().replace("-", "")}"
            
            val expiresAt = request.expires_in?.let { now.plusSeconds(it.toLong()) }
            
            val tokenId = AccessTokens.insert {
                it[AccessTokens.accountId] = accountId
                it[AccessTokens.applicationId] = applicationId
                it[AccessTokens.token] = token
                it[AccessTokens.name] = request.name
                it[AccessTokens.environment] = tokenEnvironment
                it[AccessTokens.permissions] = request.permissions?.let { perms -> json.encodeToString(perms) }
                it[AccessTokens.createdAt] = now
                it[AccessTokens.expiresAt] = expiresAt
            } get AccessTokens.id
            
            AppTokenResponse(
                token_id = tokenId.value.toString(),
                application_id = applicationId.toString(),
                access_token = token,
                name = request.name,
                environment = tokenEnvironment,
                permissions = request.permissions,
                created_at = now.toString(),
                expires_at = expiresAt?.toString(),
                last_used_at = null
            )
        }
    }
    
    fun listAppTokens(accountId: UUID, applicationId: UUID): List<AppTokenResponse> {
        return transaction {
            // Verify application belongs to account
            val app = Applications.select { 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }.firstOrNull() ?: return@transaction emptyList()
            
            AccessTokens.select { AccessTokens.applicationId eq applicationId }
                .map { row ->
                    AppTokenResponse(
                        token_id = row[AccessTokens.id].value.toString(),
                        application_id = applicationId.toString(),
                        access_token = row[AccessTokens.token],
                        name = row[AccessTokens.name],
                        environment = row[AccessTokens.environment],
                        permissions = row[AccessTokens.permissions]?.let { json.decodeFromString<List<String>>(it) },
                        created_at = row[AccessTokens.createdAt]?.toString() ?: "",
                        expires_at = row[AccessTokens.expiresAt]?.toString(),
                        last_used_at = row[AccessTokens.lastUsedAt]?.toString()
                    )
                }
        }
    }
    
    fun revokeAppToken(accountId: UUID, applicationId: UUID, tokenId: UUID): Boolean {
        return transaction {
            // Verify application belongs to account
            val app = Applications.select { 
                (Applications.id eq applicationId) and (Applications.accountId eq accountId)
            }.firstOrNull() ?: return@transaction false
            
            val deleted = AccessTokens.deleteWhere { 
                (AccessTokens.id eq tokenId) and (AccessTokens.applicationId eq applicationId)
            }
            deleted > 0
        }
    }
    
    private fun generateWebhookSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return "whsec_${bytes.joinToString("") { "%02x".format(it) }}"
    }
}

