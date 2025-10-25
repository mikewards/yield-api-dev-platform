package com.flow

import com.flow.api.routes.*
import com.flow.database.DatabaseFactory
import com.flow.middleware.auth
import com.flow.middleware.cors
import com.flow.middleware.logging
import com.flow.middleware.statusPages
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()
    
    // Configure content negotiation (JSON)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Configure middleware
    cors()
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
