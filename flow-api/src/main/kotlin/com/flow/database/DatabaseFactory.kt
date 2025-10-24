package com.flow.database

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = ConfigFactory.load()
        val dbUrl = System.getenv("DATABASE_URL") ?: config.getString("database.url")
        val dbUser = System.getenv("DATABASE_USER") ?: config.getString("database.user")
        val dbPassword = System.getenv("DATABASE_PASSWORD") ?: config.getString("database.password")
        val maxPoolSize = config.getInt("database.maxPoolSize")
        
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
