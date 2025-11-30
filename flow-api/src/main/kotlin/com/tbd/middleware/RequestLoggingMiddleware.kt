package com.tbd.middleware

import com.tbd.model.RequestLogs
import com.tbd.model.AccessTokens
import com.tbd.model.Applications
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.util.UUID

// Attribute keys for storing request data
private val RequestStartTimeKey = AttributeKey<Long>("RequestStartTime")
private val RequestIdKey = AttributeKey<String>("RequestId")
private val RequestBodyKey = AttributeKey<String>("RequestBody")

// Coroutine scope for async logging (won't block request processing)
private val loggingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

// Paths to exclude from logging (health checks, etc.)
private val excludedPaths = setOf("/health", "/", "/favicon.ico")

fun Application.requestLogging() {
    // Phase 1: Capture request start time and body
    intercept(ApplicationCallPipeline.Setup) {
        val path = call.request.path()
        
        // Skip excluded paths
        if (excludedPaths.any { path.startsWith(it) }) {
            return@intercept
        }
        
        // Generate request ID
        val requestId = "req_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        call.attributes.put(RequestIdKey, requestId)
        call.attributes.put(RequestStartTimeKey, System.currentTimeMillis())
        
        // Capture request body for POST/PUT/PATCH
        if (call.request.httpMethod in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
            try {
                // Note: This requires double-receive to be enabled or using a different approach
                // For now, we'll skip body capture to avoid complexity
            } catch (e: Exception) {
                // Ignore body capture errors
            }
        }
    }
    
    // Phase 2: Log after response is sent
    intercept(ApplicationCallPipeline.Fallback) {
        val path = call.request.path()
        
        // Skip excluded paths
        if (excludedPaths.any { path.startsWith(it) }) {
            return@intercept
        }
        
        val startTime = call.attributes.getOrNull(RequestStartTimeKey) ?: return@intercept
        val requestId = call.attributes.getOrNull(RequestIdKey) ?: "req_unknown"
        val duration = (System.currentTimeMillis() - startTime).toInt()
        
        // Extract auth info to get application ID
        val authHeader = call.request.header(HttpHeaders.Authorization)
        var applicationId: UUID? = null
        var accountId: UUID? = null
        var environment = System.getenv("ENVIRONMENT") ?: "production"
        
        // Capture values needed for async logging
        val method = call.request.httpMethod.value
        val statusCode = call.response.status()?.value ?: 0
        val ipAddress = call.request.origin.remoteHost
        val userAgent = call.request.userAgent()?.take(500)
        
        if (authHeader?.startsWith("Bearer ") == true) {
            val tokenValue = authHeader.removePrefix("Bearer ")
            try {
                // Look up token to get application info (synchronous, in same transaction context)
                val tokenData = kotlinx.coroutines.runBlocking {
                    newSuspendedTransaction {
                        AccessTokens
                            .select { AccessTokens.token eq tokenValue }
                            .firstOrNull()
                            ?.let { row ->
                                Triple(
                                    row[AccessTokens.applicationId],
                                    row[AccessTokens.accountId],
                                    row[AccessTokens.environment]
                                )
                            }
                    }
                }
                
                if (tokenData != null) {
                    applicationId = tokenData.first
                    accountId = tokenData.second
                    environment = tokenData.third
                    println("📝 Logging request for app=${applicationId}, account=${accountId}, env=${environment}")
                } else {
                    println("⚠️ Token not found for logging: ${tokenValue.take(20)}...")
                }
            } catch (e: Exception) {
                println("⚠️ Token lookup failed for logging: ${e.message}")
            }
        }
        
        // Capture final values for async logging
        val finalAppId = applicationId
        val finalAccountId = accountId
        val finalEnv = environment
        
        // Log asynchronously (non-blocking)
        loggingScope.launch {
            try {
                newSuspendedTransaction {
                    RequestLogs.insert {
                        it[RequestLogs.id] = UUID.randomUUID()
                        it[RequestLogs.requestId] = requestId
                        it[RequestLogs.applicationId] = finalAppId
                        it[RequestLogs.accountId] = finalAccountId
                        it[RequestLogs.environment] = finalEnv
                        it[RequestLogs.method] = method
                        it[RequestLogs.path] = path
                        it[RequestLogs.statusCode] = statusCode
                        it[RequestLogs.durationMs] = duration
                        it[RequestLogs.ipAddress] = ipAddress
                        it[RequestLogs.userAgent] = userAgent
                        it[RequestLogs.requestBody] = null
                        it[RequestLogs.responseBody] = null
                        it[RequestLogs.errorMessage] = if (statusCode >= 400) "HTTP $statusCode" else null
                        it[RequestLogs.timestamp] = Instant.now()
                    }
                }
                println("✅ Request logged: $method $path -> $statusCode (${duration}ms)")
            } catch (e: Exception) {
                println("❌ Failed to log request: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

