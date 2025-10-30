package com.flow.database

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = ConfigFactory.load()
        
        // Read from environment variables first, then config file
        // Trim all values to handle accidental whitespace from Railway UI
        var dbUrl = System.getenv("DATABASE_URL")?.trim()
            ?: (if (config.hasPath("database.url")) config.getString("database.url").trim() else null)
        val dbUser = System.getenv("DATABASE_USER")?.trim()
            ?: (if (config.hasPath("database.user")) config.getString("database.user").trim() else "postgres")
        val dbPassword = System.getenv("DATABASE_PASSWORD")?.trim()
            ?: (if (config.hasPath("database.password")) config.getString("database.password").trim() else "")
        val maxPoolSize = if (config.hasPath("database.maxPoolSize")) config.getInt("database.maxPoolSize") else 10
        
        // Validate required fields
        requireNotNull(dbUrl) { 
            "❌ DATABASE_URL is missing! Please set DATABASE_URL in Railway (flow-platform service → Variables). " +
            "Copy the value from flow-db service → Variables → DATABASE_URL"
        }
        
        // Convert Railway's postgresql:// format to jdbc:postgresql:// if needed
        var jdbcUrl = dbUrl.trim()
        
        // Check if empty after trimming
        if (jdbcUrl.isEmpty()) {
            throw IllegalArgumentException(
                "❌ DATABASE_URL is empty! Please set DATABASE_URL in Railway (flow-platform service → Variables). " +
                "Copy the value from flow-db service → Variables → DATABASE_URL"
            )
        }
        
        if (jdbcUrl.startsWith("postgresql://")) {
            jdbcUrl = jdbcUrl.replace("postgresql://", "jdbc:postgresql://")
        } else if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw IllegalArgumentException(
                "❌ DATABASE_URL format error! Must start with 'postgresql://' or 'jdbc:postgresql://'. " +
                "Got: ${if (jdbcUrl.length > 50) jdbcUrl.take(50) + "..." else jdbcUrl}"
            )
        }
        
        // Validate URL format
        if (!jdbcUrl.contains("@") || !jdbcUrl.contains(":")) {
            throw IllegalArgumentException(
                "❌ DATABASE_URL appears malformed! Expected format: postgresql://user:password@host:port/database. " +
                "Got: ${if (jdbcUrl.length > 100) jdbcUrl.take(100) + "..." else jdbcUrl}"
            )
        }
        
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = jdbcUrl
            username = dbUser
            password = dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
        
        // Create tables (with error handling)
        try {
            transaction {
                Schema.createTables()
            }
            println("✅ Database tables created successfully")
        } catch (e: Exception) {
            println("⚠️  Warning: Error creating tables: ${e.message}")
            // If tables already exist, that's okay - continue
            if (!e.message?.contains("already exists")!!) {
                throw e
            }
        }
    }
}
