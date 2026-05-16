package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.RcacMigrationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

/**
 * Migration DTOs
 */
@Serializable
data class MigrationStatusResponse(
    val total_accounts: Long,
    val total_users: Long,
    val total_businesses: Long,
    val total_resource_access_records: Long,
    val applications_migrated: String,
    val migration_complete: Boolean
)

@Serializable
data class MigrationRunResponse(
    val status: String,
    val message: String,
    val accounts_migrated: Int,
    val users_created: Int,
    val businesses_created: Int,
    val resource_access_records: Int,
    val errors: List<String> = emptyList()
)

/**
 * Migration endpoints for RCAC data migration.
 * 
 * IMPORTANT: These routes should be protected and only accessible
 * by system administrators. In production, consider:
 * 1. IP whitelisting
 * 2. Separate admin auth
 * 3. Feature flags
 * 4. Removing these routes after migration is complete
 */
fun Application.migrationRoutes() {
    val migrationService = RcacMigrationService()
    
    routing {
        route("/v1/admin/migration") {
            // For safety, require bearer auth (any valid API key)
            authenticate("bearer-auth") {
                
                // Get migration status
                get("/status") {
                    try {
                        val status = migrationService.getMigrationStatus()
                        call.respond(status)
                    } catch (e: Exception) {
                        call.application.environment.log.error("Migration status error", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(ErrorDetail("MIGRATION_ERROR", e.message ?: "Failed to get migration status", "server_error"))
                        )
                    }
                }
                
                // Run migration (POST for safety - not idempotent in the strictest sense)
                post("/run") {
                    try {
                        call.application.environment.log.info("Starting RCAC migration...")
                        
                        val result = migrationService.migrate()
                        
                        val response = MigrationRunResponse(
                            status = if (result.errors.isEmpty()) "success" else "partial",
                            message = if (result.errors.isEmpty()) 
                                "RCAC migration completed successfully" 
                            else 
                                "RCAC migration completed with errors",
                            accounts_migrated = result.accountsMigrated,
                            users_created = result.usersMigrated,
                            businesses_created = result.businessesCreated,
                            resource_access_records = result.resourceAccessRecordsCreated,
                            errors = result.errors
                        )
                        
                        call.respond(HttpStatusCode.OK, response)
                    } catch (e: Exception) {
                        call.application.environment.log.error("Migration error", e)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse(ErrorDetail("MIGRATION_ERROR", e.message ?: "Migration failed", "server_error"))
                        )
                    }
                }
            }
        }
    }
}

