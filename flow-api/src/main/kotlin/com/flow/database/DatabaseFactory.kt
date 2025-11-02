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
        val envDbUrl = System.getenv("DATABASE_URL")
        println("🔍 DEBUG: DATABASE_URL from env = ${if (envDbUrl == null) "NULL" else if (envDbUrl.isEmpty()) "EMPTY" else "${envDbUrl.take(30)}..."}")
        
        var dbUrl = envDbUrl?.trim()
            ?: (if (config.hasPath("database.url")) config.getString("database.url").trim() else null)
        
        println("🔍 DEBUG: dbUrl after processing = ${if (dbUrl == null) "NULL" else if (dbUrl.isEmpty()) "EMPTY" else "${dbUrl.take(30)}..."}")
        
        val dbUser = System.getenv("DATABASE_USER")?.trim()
            ?: (if (config.hasPath("database.user")) config.getString("database.user").trim() else "postgres")
        val dbPassword = System.getenv("DATABASE_PASSWORD")?.trim()
            ?: (if (config.hasPath("database.password")) config.getString("database.password").trim() else "")
        val maxPoolSize = if (config.hasPath("database.maxPoolSize")) config.getInt("database.maxPoolSize") else 10
        
        // Validate required fields
        requireNotNull(dbUrl) { 
            "❌ DATABASE_URL is missing! Railway should auto-set this when PostgreSQL service is linked. " +
            "Go to flow-platform → Settings → Service Dependencies → Add flow-db service. " +
            "Railway will automatically set DATABASE_URL."
        }
        
        // Parse DATABASE_URL to extract components
        // Format: postgresql://user:password@host:port/database
        var jdbcUrl = dbUrl.trim()
        
        println("🔍 DEBUG: jdbcUrl before conversion = ${if (jdbcUrl.isEmpty()) "EMPTY" else "${jdbcUrl.take(50)}..."}")
        
        // Check if empty after trimming
        if (jdbcUrl.isEmpty()) {
            throw IllegalArgumentException(
                "❌ DATABASE_URL is empty! Railway should auto-set this when PostgreSQL service is linked."
            )
        }
        
        // Parse the URL to extract components
        val urlPattern = Regex("""postgresql://([^:]+):([^@]+)@([^:]+):(\d+)/(.+)""")
        val match = urlPattern.find(jdbcUrl)
        
        if (match == null) {
            throw IllegalArgumentException(
                "❌ DATABASE_URL format error! Expected: postgresql://user:password@host:port/database. " +
                "Got: ${if (jdbcUrl.length > 100) jdbcUrl.take(100) + "..." else jdbcUrl}"
            )
        }
        
        val (urlUser, urlPassword, host, port, database) = match.destructured
        
        // Use username/password from URL if DATABASE_USER/DATABASE_PASSWORD not set
        val finalUser = if (dbUser == "postgres" && urlUser.isNotEmpty()) urlUser else dbUser
        val finalPassword = if (dbPassword.isEmpty() && urlPassword.isNotEmpty()) urlPassword else dbPassword
        
        // Build clean JDBC URL without credentials (we'll set them separately)
        val cleanJdbcUrl = "jdbc:postgresql://$host:$port/$database"
        
        println("🔍 DEBUG: Parsed - host=$host, port=$port, database=$database")
        println("🔍 DEBUG: Final jdbcUrl (clean, no credentials) = $cleanJdbcUrl")
        println("🔍 DEBUG: Final user = $finalUser")
        println("🔍 DEBUG: Final password = ${if (finalPassword.isEmpty()) "EMPTY" else "***"}")
        
        // Create HikariConfig and set properties explicitly
        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = cleanJdbcUrl
        hikariConfig.username = finalUser
        hikariConfig.password = finalPassword
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.maximumPoolSize = maxPoolSize
        hikariConfig.isAutoCommit = false
        hikariConfig.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        
        println("🔍 DEBUG: HikariConfig.jdbcUrl = ${if (hikariConfig.jdbcUrl.isNullOrEmpty()) "NULL/EMPTY" else "${hikariConfig.jdbcUrl.take(80)}..."}")
        println("🔍 DEBUG: HikariConfig.driverClassName = ${hikariConfig.driverClassName}")
        println("🔍 DEBUG: About to validate HikariConfig...")
        
        hikariConfig.validate()
        
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
