package com.tbd

import com.tbd.api.routes.*
import com.tbd.database.DatabaseFactory
import com.tbd.middleware.auth
import com.tbd.middleware.cors
import com.tbd.middleware.logging
import com.tbd.middleware.rateLimit
import com.tbd.middleware.sentry
import com.tbd.middleware.statusPages
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
    } catch (e: Exception) {
        println("❌ CRITICAL: Database initialization failed: ${e.message}")
        e.printStackTrace()
        // Continue anyway - health check will show DB status
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
    
    // Configure routes
    healthRoutes()
    accountRoutes()
    authRoutes()
    applicationRoutes()
    walletRoutes()
    tokenRoutes()
    yieldAccountRoutes()
    positionRoutes()
    marketRoutes()
    transactionRoutes()
    webhookRoutes()
}
