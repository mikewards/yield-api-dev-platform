package com.tbd.middleware

import com.tbd.model.RequestLogs
import com.tbd.model.AccessTokens
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

// Attribute key for storing request start time
private val RequestStartTimeKey = AttributeKey<Long>("RequestStartTime")

// Coroutine scope for async logging (won't block request processing)
private val loggingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

// Paths to exclude from logging
private val excludedPaths = listOf("/health", "/favicon.ico")

fun Application.requestLogging() {
    println("🔧 Installing request logging middleware...")
    
    // Use intercept on Call phase to capture timing and log after response
    intercept(ApplicationCallPipeline.Monitoring) {
        val path = call.request.path()
        
        // Skip excluded paths
        if (excludedPaths.any { path == it || path.startsWith("$it/") }) {
            return@intercept
        }
        
        // Record start time
        val startTime = System.currentTimeMillis()
        call.attributes.put(RequestStartTimeKey, startTime)
        
        // Continue processing - this will proceed to route handlers
        proceed()
        
        // After response is ready, log the request
        try {
            val duration = (System.currentTimeMillis() - startTime).toInt()
            val method = call.request.httpMethod.value
            val statusCode = call.response.status()?.value ?: 0
            val ipAddress = call.request.origin.remoteHost
            val userAgent = call.request.userAgent()?.take(500)
            
            println("📝 Request completed: $method $path -> $statusCode (${duration}ms)")
            
            // Extract auth info
            val authHeader = call.request.header(HttpHeaders.Authorization)
            var applicationId: UUID? = null
            var accountId: UUID? = null
            var environment = System.getenv("ENVIRONMENT") ?: "production"
            
            if (authHeader?.startsWith("Bearer ") == true) {
                val tokenValue = authHeader.removePrefix("Bearer ")
                try {
                    val tokenData = runBlocking {
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
                        println("   📌 Found token: app=${applicationId}, account=${accountId}, env=${environment}")
                    }
                } catch (e: Exception) {
                    println("   ⚠️ Token lookup error: ${e.message}")
                }
            }
            
            // Capture final values
            val finalAppId = applicationId
            val finalAccountId = accountId
            val finalEnv = environment
            val finalPath = path
            val finalMethod = method
            val finalStatus = statusCode
            val finalDuration = duration
            val finalIp = ipAddress
            val finalUa = userAgent
            
            // Log asynchronously
            loggingScope.launch {
                try {
                    newSuspendedTransaction {
                        RequestLogs.insert {
                            it[RequestLogs.id] = UUID.randomUUID()
                            it[RequestLogs.requestId] = "req_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                            it[RequestLogs.applicationId] = finalAppId
                            it[RequestLogs.accountId] = finalAccountId
                            it[RequestLogs.environment] = finalEnv
                            it[RequestLogs.method] = finalMethod
                            it[RequestLogs.path] = finalPath
                            it[RequestLogs.statusCode] = finalStatus
                            it[RequestLogs.durationMs] = finalDuration
                            it[RequestLogs.ipAddress] = finalIp
                            it[RequestLogs.userAgent] = finalUa
                            it[RequestLogs.requestBody] = null
                            it[RequestLogs.responseBody] = null
                            it[RequestLogs.errorMessage] = if (finalStatus >= 400) "HTTP $finalStatus" else null
                            it[RequestLogs.timestamp] = Instant.now()
                        }
                    }
                    println("   ✅ Logged to database")
                } catch (e: Exception) {
                    println("   ❌ Database insert failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Request logging error: ${e.message}")
        }
    }
    
    println("✅ Request logging middleware installed")
}

