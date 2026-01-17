package com.ground.api.routes

import com.ground.dto.*
import com.ground.service.RcacMigrationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
                        
                        if (result.errors.isEmpty()) {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "success",
                                    "message" to "RCAC migration completed successfully",
                                    "accounts_migrated" to result.accountsMigrated,
                                    "users_created" to result.usersMigrated,
                                    "businesses_created" to result.businessesCreated,
                                    "resource_access_records" to result.resourceAccessRecordsCreated
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.OK,
                                mapOf(
                                    "status" to "partial",
                                    "message" to "RCAC migration completed with errors",
                                    "accounts_migrated" to result.accountsMigrated,
                                    "users_created" to result.usersMigrated,
                                    "businesses_created" to result.businessesCreated,
                                    "resource_access_records" to result.resourceAccessRecordsCreated,
                                    "errors" to result.errors
                                )
                            )
                        }
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

