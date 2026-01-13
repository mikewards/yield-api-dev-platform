package com.ground.service

import com.ground.dto.*
import com.ground.integration.ProtocolService
import com.ground.model.ResourceAccess
import com.ground.model.YieldAccounts
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.util.*

class YieldService {
    private val protocolService = ProtocolService()
    private val marketService = com.ground.service.MorphoMarketService()
    private val permissionService = PermissionService()
    
    // ═══════════════════════════════════════════════════════════════
    // RCAC-AWARE METHODS
    // ═══════════════════════════════════════════════════════════════
    
    fun createYieldAccountRcac(userId: UUID, request: CreateYieldAccountRequest): YieldAccountResponse {
        return transaction {
            val protocol = when (request.protocol_preference) {
                "morpho" -> "morpho"
                "aave" -> "aave"
                else -> protocolService.selectBestProtocolSync(request.currency)
            }
            
            val now = java.time.Instant.now()
            val yieldAccountId = YieldAccounts.insert {
                it[YieldAccounts.createdBy] = userId
                it[YieldAccounts.accountId] = null
                it[YieldAccounts.currency] = request.currency
                it[YieldAccounts.protocol] = protocol
                it[YieldAccounts.annualYieldRate] = BigDecimal("0.06")
                it[YieldAccounts.status] = "active"
                it[YieldAccounts.balance] = BigDecimal.ZERO
                it[YieldAccounts.createdAt] = now
                it[YieldAccounts.updatedAt] = now
            } get YieldAccounts.id
            
            val initialBalance = if (request.initial_deposit != null) {
                BigDecimal(request.initial_deposit.amount)
            } else {
                BigDecimal.ZERO
            }
            
            if (initialBalance > BigDecimal.ZERO) {
                YieldAccounts.update({ YieldAccounts.id eq yieldAccountId }) {
                    it[YieldAccounts.balance] = initialBalance
                }
            }
            
            val account = YieldAccounts.select { YieldAccounts.id eq yieldAccountId }.first()
            
            // Grant admin access to creator
            permissionService.grantUserAccess(
                resourceType = "yield_account",
                resourceId = yieldAccountId.value,
                userId = userId,
                permission = "admin",
                grantedBy = userId,
                reason = "Resource creator"
            )
            
            account.toYieldAccountResponse()
        }
    }
    
    fun listYieldAccountsRcac(userId: UUID): List<YieldAccountResponse> {
        val accessibleIds = permissionService.getAccessibleResourceIds(userId, "yield_account", "read")
        return transaction {
            YieldAccounts.select { YieldAccounts.id inList accessibleIds }
                .map { it.toYieldAccountResponse() }
        }
    }
    
    fun getYieldAccountRcac(userId: UUID, yieldAccountId: UUID, requiredPermission: String = "read"): YieldAccountResponse? {
        if (!permissionService.canAccessResource(userId, "yield_account", yieldAccountId, requiredPermission)) {
            return null
        }
        return getYieldAccountById(yieldAccountId)
    }
    
    fun depositRcac(userId: UUID, yieldAccountId: UUID, request: DepositRequest): TransactionResponse {
        if (!permissionService.canAccessResource(userId, "yield_account", yieldAccountId, "write")) {
            throw IllegalArgumentException("Yield account not found or access denied")
        }
        
        return transaction {
            val yieldAccount = YieldAccounts.select { YieldAccounts.id eq yieldAccountId }
                .firstOrNull() ?: throw IllegalArgumentException("Yield account not found")
            
            if (yieldAccount[YieldAccounts.currency] != request.currency) {
                throw IllegalArgumentException("Currency does not match account currency")
            }
            
            val amount = BigDecimal(request.amount)
            val now = java.time.Instant.now()
            
            val transactionId = com.ground.model.Transactions.insert {
                it[com.ground.model.Transactions.createdBy] = userId
                it[com.ground.model.Transactions.accountId] = null
                it[com.ground.model.Transactions.yieldAccountId] = yieldAccountId
                it[com.ground.model.Transactions.type] = "deposit"
                it[com.ground.model.Transactions.status] = "pending"
                it[com.ground.model.Transactions.amount] = amount
                it[com.ground.model.Transactions.currency] = request.currency
                it[com.ground.model.Transactions.sourceAddress] = request.source_address
                it[com.ground.model.Transactions.createdAt] = now
            } get com.ground.model.Transactions.id
            
            YieldAccounts.update({ YieldAccounts.id eq yieldAccountId }) {
                it[YieldAccounts.balance] = yieldAccount[YieldAccounts.balance] + amount
            }
            
            com.ground.model.Transactions.update({ com.ground.model.Transactions.id eq transactionId }) {
                it[com.ground.model.Transactions.status] = "completed"
                it[com.ground.model.Transactions.completedAt] = java.time.Instant.now()
            }
            
            TransactionResponse(
                transaction_id = transactionId.value.toString(),
                account_id = userId.toString(),
                amount = Money(amount = request.amount, currency = request.currency),
                status = "completed",
                created_at = now.toString()
            )
        }
    }
    
    fun withdrawRcac(userId: UUID, yieldAccountId: UUID, request: WithdrawRequest): TransactionResponse {
        if (!permissionService.canAccessResource(userId, "yield_account", yieldAccountId, "write")) {
            throw IllegalArgumentException("Yield account not found or access denied")
        }
        
        return transaction {
            val yieldAccount = YieldAccounts.select { YieldAccounts.id eq yieldAccountId }
                .firstOrNull() ?: throw IllegalArgumentException("Yield account not found")
            
            if (yieldAccount[YieldAccounts.currency] != request.currency) {
                throw IllegalArgumentException("Currency does not match account currency")
            }
            
            val amount = BigDecimal(request.amount)
            
            if (yieldAccount[YieldAccounts.balance] < amount) {
                throw IllegalArgumentException("Insufficient balance for withdrawal")
            }
            
            val now = java.time.Instant.now()
            val transactionId = com.ground.model.Transactions.insert {
                it[com.ground.model.Transactions.createdBy] = userId
                it[com.ground.model.Transactions.accountId] = null
                it[com.ground.model.Transactions.yieldAccountId] = yieldAccountId
                it[com.ground.model.Transactions.type] = "withdraw"
                it[com.ground.model.Transactions.status] = "pending"
                it[com.ground.model.Transactions.amount] = amount
                it[com.ground.model.Transactions.currency] = request.currency
                it[com.ground.model.Transactions.destinationAddress] = request.destination_address
                it[com.ground.model.Transactions.createdAt] = now
            } get com.ground.model.Transactions.id
            
            YieldAccounts.update({ YieldAccounts.id eq yieldAccountId }) {
                it[YieldAccounts.balance] = yieldAccount[YieldAccounts.balance] - amount
            }
            
            com.ground.model.Transactions.update({ com.ground.model.Transactions.id eq transactionId }) {
                it[com.ground.model.Transactions.status] = "completed"
                it[com.ground.model.Transactions.completedAt] = java.time.Instant.now()
            }
            
            TransactionResponse(
                transaction_id = transactionId.value.toString(),
                account_id = userId.toString(),
                amount = Money(amount = request.amount, currency = request.currency),
                status = "completed",
                created_at = now.toString(),
                destination_address = request.destination_address
            )
        }
    }
    
    private fun getYieldAccountById(yieldAccountId: UUID): YieldAccountResponse? {
        return transaction {
            YieldAccounts.select { YieldAccounts.id eq yieldAccountId }
                .firstOrNull()?.toYieldAccountResponse()
        }
    }
    
    private fun ResultRow.toYieldAccountResponse(): YieldAccountResponse {
        return YieldAccountResponse(
            account_id = this[YieldAccounts.id].value.toString(),
            currency = this[YieldAccounts.currency],
            protocol = this[YieldAccounts.protocol],
            annual_yield_rate = this[YieldAccounts.annualYieldRate].toDouble(),
            status = this[YieldAccounts.status],
            created_at = this[YieldAccounts.createdAt]?.toString() ?: "",
            balance = Money(
                amount = this[YieldAccounts.balance].toString(),
                currency = this[YieldAccounts.currency]
            )
        )
    }
    
    // ═══════════════════════════════════════════════════════════════
    // LEGACY METHODS (Backward compatible)
    // ═══════════════════════════════════════════════════════════════
    
    fun createYieldAccount(accountId: UUID, request: CreateYieldAccountRequest): YieldAccountResponse {
        return transaction {
            // Determine protocol
            val protocol = when (request.protocol_preference) {
                "morpho" -> "morpho"
                "aave" -> "aave"
                else -> protocolService.selectBestProtocolSync(request.currency)
            }
            
            // Create yield account
            val now = java.time.Instant.now()
            val yieldAccountId = YieldAccounts.insert {
                it[YieldAccounts.accountId] = accountId
                it[YieldAccounts.currency] = request.currency
                it[YieldAccounts.protocol] = protocol
                it[YieldAccounts.annualYieldRate] = BigDecimal("0.06")
                it[YieldAccounts.status] = "active"
                it[YieldAccounts.balance] = BigDecimal.ZERO
                it[YieldAccounts.createdAt] = now
                it[YieldAccounts.updatedAt] = now
            } get YieldAccounts.id
            
            // Handle initial deposit if provided
            val initialBalance = if (request.initial_deposit != null) {
                val amount = BigDecimal(request.initial_deposit.amount)
                // In production, this would interact with blockchain/wallet
                amount
            } else {
                BigDecimal.ZERO
            }
            
            if (initialBalance > BigDecimal.ZERO) {
                YieldAccounts.update({ YieldAccounts.id eq yieldAccountId }) {
                    it[YieldAccounts.balance] = initialBalance
                }
            }
            
            val account = YieldAccounts.select { YieldAccounts.id eq yieldAccountId }.first()
            
            YieldAccountResponse(
                account_id = yieldAccountId.value.toString(),
                currency = account[YieldAccounts.currency],
                protocol = account[YieldAccounts.protocol],
                annual_yield_rate = account[YieldAccounts.annualYieldRate].toDouble(),
                status = account[YieldAccounts.status],
                created_at = account[YieldAccounts.createdAt]?.toString() ?: now.toString(),
                balance = Money(
                    amount = account[YieldAccounts.balance].toString(),
                    currency = account[YieldAccounts.currency]
                )
            )
        }
    }
    
    fun deposit(accountId: UUID, yieldAccountId: UUID, request: DepositRequest): TransactionResponse {
        return transaction {
            val yieldAccount = YieldAccounts.select { 
                YieldAccounts.id eq yieldAccountId and (YieldAccounts.accountId eq accountId)
            }.firstOrNull() ?: throw IllegalArgumentException("Yield account not found")
            
            if (yieldAccount[YieldAccounts.currency] != request.currency) {
                throw IllegalArgumentException("Currency does not match account currency")
            }
            
            val amount = BigDecimal(request.amount)
            
            // Create transaction
            val now = java.time.Instant.now()
            val transactionId = com.ground.model.Transactions.insert {
                it[com.ground.model.Transactions.accountId] = accountId
                it[com.ground.model.Transactions.yieldAccountId] = yieldAccountId
                it[com.ground.model.Transactions.type] = "deposit"
                it[com.ground.model.Transactions.status] = "pending"
                it[com.ground.model.Transactions.amount] = amount
                it[com.ground.model.Transactions.currency] = request.currency
                it[com.ground.model.Transactions.sourceAddress] = request.source_address
                it[com.ground.model.Transactions.createdAt] = now
            } get com.ground.model.Transactions.id
            
            // Update balance (in production, this would wait for blockchain confirmation)
            YieldAccounts.update({ YieldAccounts.id eq yieldAccountId }) {
                it[YieldAccounts.balance] = yieldAccount[YieldAccounts.balance] + amount
            }
            
            // Update transaction status
            com.ground.model.Transactions.update({ com.ground.model.Transactions.id eq transactionId }) {
                it[com.ground.model.Transactions.status] = "completed"
                it[com.ground.model.Transactions.completedAt] = java.time.Instant.now()
            }
            
            TransactionResponse(
                transaction_id = transactionId.value.toString(),
                account_id = accountId.toString(),
                amount = Money(amount = request.amount, currency = request.currency),
                status = "completed",
                created_at = com.ground.model.Transactions.select { 
                    com.ground.model.Transactions.id eq transactionId 
                }.first()[com.ground.model.Transactions.createdAt]?.toString() ?: now.toString()
            )
        }
    }
    
    fun withdraw(accountId: UUID, yieldAccountId: UUID, request: WithdrawRequest): TransactionResponse {
        return transaction {
            val yieldAccount = YieldAccounts.select { 
                YieldAccounts.id eq yieldAccountId and (YieldAccounts.accountId eq accountId)
            }.firstOrNull() ?: throw IllegalArgumentException("Yield account not found")
            
            if (yieldAccount[YieldAccounts.currency] != request.currency) {
                throw IllegalArgumentException("Currency does not match account currency")
            }
            
            val amount = BigDecimal(request.amount)
            
            if (yieldAccount[YieldAccounts.balance] < amount) {
                throw IllegalArgumentException("Insufficient balance for withdrawal")
            }
            
            // Create transaction
            val now = java.time.Instant.now()
            val transactionId = com.ground.model.Transactions.insert {
                it[com.ground.model.Transactions.accountId] = accountId
                it[com.ground.model.Transactions.yieldAccountId] = yieldAccountId
                it[com.ground.model.Transactions.type] = "withdraw"
                it[com.ground.model.Transactions.status] = "pending"
                it[com.ground.model.Transactions.amount] = amount
                it[com.ground.model.Transactions.currency] = request.currency
                it[com.ground.model.Transactions.destinationAddress] = request.destination_address
                it[com.ground.model.Transactions.createdAt] = now
            } get com.ground.model.Transactions.id
            
            // Update balance (in production, this would initiate blockchain transfer)
            YieldAccounts.update({ YieldAccounts.id eq yieldAccountId }) {
                it[YieldAccounts.balance] = yieldAccount[YieldAccounts.balance] - amount
            }
            
            // Update transaction status
            com.ground.model.Transactions.update({ com.ground.model.Transactions.id eq transactionId }) {
                it[com.ground.model.Transactions.status] = "completed"
                it[com.ground.model.Transactions.completedAt] = java.time.Instant.now()
            }
            
            TransactionResponse(
                transaction_id = transactionId.value.toString(),
                account_id = accountId.toString(),
                amount = Money(amount = request.amount, currency = request.currency),
                status = "completed",
                created_at = com.ground.model.Transactions.select { 
                    com.ground.model.Transactions.id eq transactionId 
                }.first()[com.ground.model.Transactions.createdAt]?.toString() ?: now.toString(),
                destination_address = request.destination_address
            )
        }
    }
    
    fun getYieldAccount(accountId: UUID, yieldAccountId: UUID): YieldAccountResponse? {
        return transaction {
            YieldAccounts.select { 
                YieldAccounts.id eq yieldAccountId and (YieldAccounts.accountId eq accountId)
            }.firstOrNull()?.let {
                YieldAccountResponse(
                    account_id = it[YieldAccounts.id].value.toString(),
                    currency = it[YieldAccounts.currency],
                    protocol = it[YieldAccounts.protocol],
                    annual_yield_rate = it[YieldAccounts.annualYieldRate].toDouble(),
                    status = it[YieldAccounts.status],
                    created_at = it[YieldAccounts.createdAt].toString(),
                    balance = Money(
                        amount = it[YieldAccounts.balance].toString(),
                        currency = it[YieldAccounts.currency]
                    )
                )
            }
        }
    }
    
    fun listYieldAccounts(accountId: UUID): List<YieldAccountResponse> {
        return transaction {
            YieldAccounts.select { YieldAccounts.accountId eq accountId }
                .map {
                    YieldAccountResponse(
                        account_id = it[YieldAccounts.id].value.toString(),
                        currency = it[YieldAccounts.currency],
                        protocol = it[YieldAccounts.protocol],
                        annual_yield_rate = it[YieldAccounts.annualYieldRate].toDouble(),
                        status = it[YieldAccounts.status],
                        created_at = it[YieldAccounts.createdAt].toString(),
                        balance = Money(
                            amount = it[YieldAccounts.balance].toString(),
                            currency = it[YieldAccounts.currency]
                        )
                    )
                }
        }
    }
}
