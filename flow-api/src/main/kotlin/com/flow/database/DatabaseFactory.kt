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
        val dbUrl = System.getenv("DATABASE_URL") 
            ?: (if (config.hasPath("database.url")) config.getString("database.url") else null)
        val dbUser = System.getenv("DATABASE_USER") 
            ?: (if (config.hasPath("database.user")) config.getString("database.user") else "postgres")
        val dbPassword = System.getenv("DATABASE_PASSWORD") 
            ?: (if (config.hasPath("database.password")) config.getString("database.password") else "")
        val maxPoolSize = if (config.hasPath("database.maxPoolSize")) config.getInt("database.maxPoolSize") else 10
        
        // Validate required fields
        requireNotNull(dbUrl) { "DATABASE_URL environment variable or database.url config is required" }
        
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbUrl
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
        
        // Create tables
        transaction {
            Schema.createTables()
        }
    }
}
