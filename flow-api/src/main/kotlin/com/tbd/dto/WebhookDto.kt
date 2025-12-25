package com.tbd.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateWebhookEndpointRequest(
    val url: String,
    val description: String? = null,
    @SerialName("filter_types")
    val filterTypes: List<String>? = null
)

@Serializable
data class WebhookEndpointResponse(
    val id: String,
    val url: String,
    val description: String?,
    @SerialName("filter_types")
    val filterTypes: List<String>?,
    @SerialName("created_at")
    val createdAt: String?,
    @SerialName("updated_at")
    val updatedAt: String?,
    val disabled: Boolean
)

@Serializable
data class WebhookEndpointsListResponse(
    val endpoints: List<WebhookEndpointResponse>,
    val total: Int
)

@Serializable
data class WebhookEventTypesResponse(
    @SerialName("event_types")
    val eventTypes: List<WebhookEventType>
)

@Serializable
data class WebhookEventType(
    val name: String,
    val description: String
)

@Serializable
data class TestWebhookRequest(
    @SerialName("event_type")
    val eventType: String
)

@Serializable
data class TestWebhookResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class WebhookStatusResponse(
    val available: Boolean,
    val message: String
)
