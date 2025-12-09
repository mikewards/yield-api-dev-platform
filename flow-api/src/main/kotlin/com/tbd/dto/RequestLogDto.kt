package com.tbd.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestLogResponse(
    val id: String,
    val request_id: String,
    val application_id: String?,
    val application_name: String?,
    val environment: String,
    val method: String,
    val path: String,
    val status_code: Int,
    val duration_ms: Int,
    val ip_address: String?,
    val user_agent: String?,
    val request_body: String?,
    val response_body: String?,
    val error_message: String?,
    val timestamp: String
)

@Serializable
data class RequestLogsListResponse(
    val logs: List<RequestLogResponse>,
    val total: Int,
    val page: Int,
    val page_size: Int,
    val stats: RequestLogStats
)

@Serializable
data class RequestLogStats(
    val total_requests_7d: Int,
    val success_rate: Double,
    val avg_duration_ms: Int,
    val errors_24h: Int
)

