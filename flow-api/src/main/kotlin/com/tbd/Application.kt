package com.tbd

import com.tbd.api.routes.*
import com.tbd.database.DatabaseFactory
import com.tbd.middleware.auth
import com.tbd.middleware.cors
import com.tbd.middleware.logging
import com.tbd.middleware.rateLimit
import com.tbd.middleware.requestLogging
import com.tbd.middleware.sentry
import com.tbd.middleware.statusPages
import com.tbd.service.AccountCleanupJob
import com.tbd.service.LogCleanupJob
import com.tbd.service.WebhookService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Initialize Sentry (if DSN is provided)
    sentry()
    
    // Initialize database (with error handling - don't crash if DB fails)
    try {
        DatabaseFactory.init()
        // Start background jobs
        LogCleanupJob.start()
        AccountCleanupJob.start()
    } catch (e: Exception) {
        println("❌ CRITICAL: Database initialization failed: ${e.message}")
        e.printStackTrace()
        // Continue anyway - health check will show DB status
    }
    
    // Initialize Svix webhook event types
    try {
        WebhookService.initializeEventTypes()
    } catch (e: Exception) {
        println("⚠️ Webhook event type initialization failed: ${e.message}")
        // Continue anyway - webhooks may still work for existing event types
    }
    
    // Configure content negotiation (JSON)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Configure middleware (order matters!)
    cors()
    rateLimit()  // Rate limiting early in pipeline
    logging()
    statusPages()
    auth()
    requestLogging()  // Log requests after auth (so we have user context)
    
    // Configure routes
    healthRoutes()
    accountRoutes()
    authRoutes()
    accountSettingsRoutes()
    applicationRoutes()
    walletRoutes()
    tokenRoutes()
    yieldAccountRoutes()
    positionRoutes()
    marketRoutes()
    transactionRoutes()
    webhookRoutes()
    logRoutes()
}
