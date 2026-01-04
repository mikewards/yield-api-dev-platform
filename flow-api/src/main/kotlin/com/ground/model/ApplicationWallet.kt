package com.ground.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

object ApplicationWallets : UUIDTable("application_wallets") {
    val applicationId = uuid("application_id").references(Applications.id)
    val address = varchar("address", 42).uniqueIndex() // Ethereum address (0x...)
    val encryptedPrivateKey = text("encrypted_private_key") // AES-encrypted private key
    val chain = varchar("chain", 20).default("ethereum") // ethereum, polygon, arbitrum
    val environment = varchar("environment", 20).default("sandbox") // sandbox, production
    val label = varchar("label", 100).nullable() // User-friendly label
    val status = varchar("status", 20).default("active") // active, inactive, archived
    val createdAt = timestamp("created_at").nullable()
    val updatedAt = timestamp("updated_at").nullable()
}

