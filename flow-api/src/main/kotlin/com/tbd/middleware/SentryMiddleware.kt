package com.tbd.middleware

import io.sentry.Sentry
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.http.*
import com.tbd.dto.ErrorResponse
import com.tbd.dto.ErrorDetail

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
            options.tracesSampleRate = 0.1 // 10% of transactions
            options.isDebug = false
            options.setTag("service", "tbd-api")
        }
        println("✅ Sentry initialized successfully")
    } catch (e: Exception) {
        println("⚠️  Failed to initialize Sentry: ${e.message}")
    }
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

