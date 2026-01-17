package com.ground

import com.ground.api.routes.*
import com.ground.database.DatabaseFactory
import com.ground.middleware.auth
import com.ground.middleware.cors
import com.ground.middleware.logging
import com.ground.middleware.rateLimit
import com.ground.middleware.requestLogging
import com.ground.middleware.sentry
import com.ground.middleware.sentryPerformance
import com.ground.middleware.statusPages
import com.ground.service.AccountCleanupJob
import com.ground.service.LogCleanupJob
import com.ground.service.WebhookService
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
    sentryPerformance()
    
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
    
    // RCAC routes (new user/business model)
    userAuthRoutes()
    businessRoutes()
    resourceAccessRoutes()
    
    // Legacy routes (still using Account model, will be migrated)
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
