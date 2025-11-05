package com.tbd.database

import com.tbd.model.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Schema {
    fun createTables() {
        transaction {
            try {
                SchemaUtils.createMissingTablesAndColumns(
                    Accounts,
                    Applications,
                    ApplicationWallets,
                    AccessTokens,
                    YieldAccounts,
                    Positions,
                    Transactions,
                    Webhooks
                )
            } catch (e: Exception) {
                // If createMissingTablesAndColumns fails, try create (for new databases)
                println("Creating tables with create()...")
                SchemaUtils.create(
                    Accounts,
                    Applications,
                    ApplicationWallets,
                    AccessTokens,
                    YieldAccounts,
                    Positions,
                    Transactions,
                    Webhooks
                )
            }
        }
    }
}
