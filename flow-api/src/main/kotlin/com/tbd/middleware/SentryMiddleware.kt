package com.tbd.middleware

import io.sentry.Sentry
import io.sentry.SentryOptions
import io.sentry.SpanStatus
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.util.*
import com.tbd.dto.ErrorResponse
import com.tbd.dto.ErrorDetail

// Attribute key to store transaction in call
private val SentryTransactionKey = AttributeKey<io.sentry.ITransaction>("SentryTransaction")

fun Application.sentry() {
    // Initialize Sentry only if DSN is provided
    val sentryDsn = System.getenv("SENTRY_DSN")
    if (sentryDsn.isNullOrBlank()) {
        println("ℹ️  Sentry DSN not provided, error tracking disabled")
        return
    }
    
    try {
        Sentry.init { options ->
            options.dsn = sentryDsn
            options.environment = System.getenv("ENVIRONMENT") ?: "development"
            
            // Performance Monitoring - capture 100% in sandbox, 20% in production
            val env = System.getenv("ENVIRONMENT") ?: "development"
            options.tracesSampleRate = if (env == "production") 0.2 else 1.0
            
            // Enable profiling for performance insights
            options.profilesSampleRate = if (env == "production") 0.1 else 0.5
            
            options.isDebug = false
            options.setTag("service", "tbd-api")
            
            // Set release version for tracking deployments
            options.release = "tbd-api@1.0.0"
            
            // Enable sending of default PII (be careful in production)
            options.isSendDefaultPii = false
        }
        println("✅ Sentry initialized with performance monitoring (traces: ${if (System.getenv("ENVIRONMENT") == "production") "20%" else "100%"})")
    } catch (e: Exception) {
        println("⚠️  Failed to initialize Sentry: ${e.message}")
    }
}

/**
 * Sentry performance monitoring interceptor
 * Creates a transaction for each HTTP request
 */
fun Application.sentryPerformance() {
    val sentryDsn = System.getenv("SENTRY_DSN")
    if (sentryDsn.isNullOrBlank()) return
    
    intercept(ApplicationCallPipeline.Monitoring) {
        val request = call.request
        val transactionName = "${request.httpMethod.value} ${request.path()}"
        
        // Start a new transaction
        val transaction = Sentry.startTransaction(transactionName, "http.server")
        transaction.setTag("http.method", request.httpMethod.value)
        transaction.setTag("http.url", request.path())
        
        // Store transaction in call attributes
        call.attributes.put(SentryTransactionKey, transaction)
        
        try {
            proceed()
            
            // Set status based on response
            val statusCode = call.response.status()?.value ?: 200
            transaction.setTag("http.status_code", statusCode.toString())
            transaction.status = when {
                statusCode < 400 -> SpanStatus.OK
                statusCode < 500 -> SpanStatus.INVALID_ARGUMENT
                else -> SpanStatus.INTERNAL_ERROR
            }
        } catch (e: Exception) {
            transaction.status = SpanStatus.INTERNAL_ERROR
            transaction.throwable = e
            throw e
        } finally {
            transaction.finish()
        }
    }
    
    println("✅ Sentry performance monitoring enabled")
}

/**
 * Capture exception to Sentry (call this from status pages)
 */
fun captureException(exception: Throwable, context: Map<String, String> = emptyMap()) {
    try {
        Sentry.configureScope { scope ->
            context.forEach { (key, value) ->
                scope.setTag(key, value)
            }
        }
        Sentry.captureException(exception)
    } catch (e: Exception) {
        // Silently fail if Sentry is not initialized
        println("⚠️  Failed to capture exception to Sentry: ${e.message}")
    }
}

/**
 * Set user context for Sentry (IDs only - no PII)
 * Call this after successful JWT authentication
 */
fun setSentryUserContext(accountId: String) {
    try {
        Sentry.configureScope { scope ->
            scope.setUser(io.sentry.protocol.User().apply {
                id = accountId
            })
            scope.setTag("auth_type", "jwt")
        }
    } catch (e: Exception) {
        // Silently fail if Sentry is not initialized
    }
}

/**
 * Set application context for Sentry (IDs only - no PII)
 * Call this after successful API key (PAT) authentication
 */
fun setSentryApplicationContext(accountId: String, applicationId: String, environment: String) {
    try {
        Sentry.configureScope { scope ->
            scope.setUser(io.sentry.protocol.User().apply {
                id = accountId
            })
            scope.setTag("auth_type", "api_key")
            scope.setTag("application_id", applicationId)
            scope.setTag("api_environment", environment)
        }
    } catch (e: Exception) {
        // Silently fail if Sentry is not initialized
    }
}

