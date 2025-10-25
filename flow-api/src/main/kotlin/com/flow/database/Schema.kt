package com.flow.database

import com.flow.model.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object Schema {
    fun createTables() {
        transaction {
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
