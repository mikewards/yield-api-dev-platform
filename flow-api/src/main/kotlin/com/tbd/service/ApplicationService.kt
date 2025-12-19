package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.AccessTokens
import com.tbd.model.Applications
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
            // First delete all tokens associated with this application
            AccessTokens.deleteWhere { AccessTokens.applicationId eq applicationId }
            
            // Then delete the application
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

