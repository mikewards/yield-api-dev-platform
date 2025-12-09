package com.tbd.service

import com.tbd.dto.*
import com.tbd.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RequestLogService {
    
    suspend fun getLogsForAccount(
        accountId: UUID,
        applicationId: UUID? = null,
        environment: String? = null,
        method: String? = null,
        statusFilter: String? = null,
        pathFilter: String? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): RequestLogsListResponse = newSuspendedTransaction {
        // Get all application IDs for this account
        val accountAppIds = Applications
            .select { Applications.accountId eq accountId }
            .map { it[Applications.id].value }
        
        if (accountAppIds.isEmpty()) {
            return@newSuspendedTransaction RequestLogsListResponse(
                logs = emptyList(),
                total = 0,
                page = page,
                page_size = pageSize,
                stats = RequestLogStats(0, 0.0, 0, 0)
            )
        }
        
        // Build query conditions
        var conditions: Op<Boolean> = RequestLogs.applicationId inList accountAppIds
        
        // Filter by specific application
        if (applicationId != null) {
            conditions = conditions and (RequestLogs.applicationId eq applicationId)
        }
        
        // Filter by environment
        if (!environment.isNullOrBlank()) {
            conditions = conditions and (RequestLogs.environment eq environment)
        }
        
        // Filter by method
        if (!method.isNullOrBlank()) {
            conditions = conditions and (RequestLogs.method eq method)
        }
        
        // Filter by status
        when (statusFilter) {
            "2xx" -> conditions = conditions and (RequestLogs.statusCode greaterEq 200) and (RequestLogs.statusCode less 300)
            "4xx" -> conditions = conditions and (RequestLogs.statusCode greaterEq 400) and (RequestLogs.statusCode less 500)
            "5xx" -> conditions = conditions and (RequestLogs.statusCode greaterEq 500)
        }
        
        // Filter by path
        if (!pathFilter.isNullOrBlank()) {
            conditions = conditions and (RequestLogs.path like "%$pathFilter%")
        }
        
        // Get total count
        val total = RequestLogs.select { conditions }.count().toInt()
        
        // Get paginated logs
        val logs = RequestLogs
            .leftJoin(Applications)
            .select { conditions }
            .orderBy(RequestLogs.timestamp, SortOrder.DESC)
            .limit(pageSize, ((page - 1) * pageSize).toLong())
            .map { row ->
                RequestLogResponse(
                    id = row[RequestLogs.id].value.toString(),
                    request_id = row[RequestLogs.requestId],
                    application_id = row[RequestLogs.applicationId]?.toString(),
                    application_name = row.getOrNull(Applications.name),
                    environment = row[RequestLogs.environment],
                    method = row[RequestLogs.method],
                    path = row[RequestLogs.path],
                    status_code = row[RequestLogs.statusCode],
                    duration_ms = row[RequestLogs.durationMs],
                    ip_address = row[RequestLogs.ipAddress],
                    user_agent = row[RequestLogs.userAgent],
                    request_body = row[RequestLogs.requestBody],
                    response_body = row[RequestLogs.responseBody],
                    error_message = row[RequestLogs.errorMessage],
                    timestamp = row[RequestLogs.timestamp].toString()
                )
            }
        
        // Calculate stats
        val stats = calculateStats(accountAppIds)
        
        RequestLogsListResponse(
            logs = logs,
            total = total,
            page = page,
            page_size = pageSize,
            stats = stats
        )
    }
    
    private fun calculateStats(applicationIds: List<UUID>): RequestLogStats {
        val now = Instant.now()
        val sevenDaysAgo = now.minus(7, ChronoUnit.DAYS)
        val oneDayAgo = now.minus(24, ChronoUnit.HOURS)
        
        // Total requests in last 7 days
        val total7d = RequestLogs
            .select { 
                (RequestLogs.applicationId inList applicationIds) and 
                (RequestLogs.timestamp greaterEq sevenDaysAgo) 
            }
            .count().toInt()
        
        if (total7d == 0) {
            return RequestLogStats(0, 100.0, 0, 0)
        }
        
        // Success count (2xx)
        val successCount = RequestLogs
            .select { 
                (RequestLogs.applicationId inList applicationIds) and 
                (RequestLogs.timestamp greaterEq sevenDaysAgo) and
                (RequestLogs.statusCode greaterEq 200) and 
                (RequestLogs.statusCode less 300)
            }
            .count().toInt()
        
        // Average duration
        val avgDuration = RequestLogs
            .slice(RequestLogs.durationMs.avg())
            .select { 
                (RequestLogs.applicationId inList applicationIds) and 
                (RequestLogs.timestamp greaterEq sevenDaysAgo) 
            }
            .firstOrNull()
            ?.get(RequestLogs.durationMs.avg())
            ?.toInt() ?: 0
        
        // Errors in last 24 hours
        val errors24h = RequestLogs
            .select { 
                (RequestLogs.applicationId inList applicationIds) and 
                (RequestLogs.timestamp greaterEq oneDayAgo) and
                (RequestLogs.statusCode greaterEq 400)
            }
            .count().toInt()
        
        val successRate = if (total7d > 0) {
            (successCount.toDouble() / total7d.toDouble()) * 100
        } else 100.0
        
        return RequestLogStats(
            total_requests_7d = total7d,
            success_rate = kotlin.math.round(successRate * 10) / 10,
            avg_duration_ms = avgDuration,
            errors_24h = errors24h
        )
    }
    
    suspend fun cleanupOldLogs(): Int = newSuspendedTransaction {
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
        
        RequestLogs.deleteWhere { 
            RequestLogs.timestamp less sevenDaysAgo 
        }
    }
}

