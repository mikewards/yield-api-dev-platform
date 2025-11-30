package com.tbd.middleware

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Rate Limit Configuration
 * 
 * Configure rate limits per endpoint. Each endpoint can have its own limit.
 * Default is 100 requests per minute if not specified.
 */
object RateLimitConfig {
    // Default rate limit: 100 requests per minute
    const val DEFAULT_REQUESTS_PER_MINUTE = 100
    
    // Window size in milliseconds (1 minute)
    const val WINDOW_SIZE_MS = 60_000L
    
    /**
     * Per-endpoint rate limit configuration
     * Key: "METHOD /path" (e.g., "GET /v1/yield/rates")
     * Value: requests per minute
     * 
     * Add entries here to customize rate limits for specific endpoints.
     * Endpoints not listed will use DEFAULT_REQUESTS_PER_MINUTE.
     */
    val endpointLimits: Map<String, Int> = mapOf(
        // === CONFIGURABLE RATE LIMITS ===
        // Default: 100 req/min for all endpoints
        // 
        // Reduced limits (75 req/min) for demonstration:
        "GET /v1/markets" to 75,
        "GET /v1/yield/rates" to 75,
        
        // Standard limits (100 req/min) - explicitly set for documentation
        "POST /v1/auth/authenticate" to 100,
        "POST /v1/accounts" to 100,
        "GET /v1/yield/accounts" to 100,
        "POST /v1/yield/accounts" to 100,
        "GET /v1/yield/positions" to 100,
        "GET /v1/transactions" to 100,
        "POST /v1/applications" to 100,
        "GET /v1/applications" to 100,
        
        // Health check - higher limit
        "GET /health" to 300,
        "GET /" to 300
    )
    
    /**
     * Get rate limit for a specific endpoint
     */
    fun getLimitForEndpoint(method: String, path: String): Int {
        val key = "$method $path"
        
        // Try exact match first
        endpointLimits[key]?.let { return it }
        
        // Try matching with path parameters (e.g., /v1/applications/{id})
        for ((pattern, limit) in endpointLimits) {
            if (matchesPattern(key, pattern)) {
                return limit
            }
        }
        
        // Return default
        return DEFAULT_REQUESTS_PER_MINUTE
    }
    
    private fun matchesPattern(actual: String, pattern: String): Boolean {
        val actualParts = actual.split(" ", "/").filter { it.isNotEmpty() }
        val patternParts = pattern.split(" ", "/").filter { it.isNotEmpty() }
        
        if (actualParts.size != patternParts.size) return false
        
        return actualParts.zip(patternParts).all { (a, p) ->
            p.startsWith("{") && p.endsWith("}") || a == p
        }
    }
}

/**
 * Sliding window rate limiter using in-memory storage
 * 
 * For production at scale, consider Redis-based implementation
 */
object RateLimiter {
    // Map of "IP:endpoint" -> list of request timestamps
    private val requestWindows = ConcurrentHashMap<String, MutableList<Long>>()
    
    /**
     * Check if request should be allowed
     * Returns pair of (allowed, remaining requests)
     */
    fun checkLimit(clientId: String, endpoint: String, limit: Int): RateLimitResult {
        val key = "$clientId:$endpoint"
        val now = System.currentTimeMillis()
        val windowStart = now - RateLimitConfig.WINDOW_SIZE_MS
        
        // Get or create request list for this key
        val requests = requestWindows.computeIfAbsent(key) { mutableListOf() }
        
        synchronized(requests) {
            // Remove old requests outside the window
            requests.removeIf { it < windowStart }
            
            val currentCount = requests.size
            val remaining = (limit - currentCount - 1).coerceAtLeast(0)
            val resetTime = if (requests.isNotEmpty()) {
                requests.first() + RateLimitConfig.WINDOW_SIZE_MS
            } else {
                now + RateLimitConfig.WINDOW_SIZE_MS
            }
            
            return if (currentCount < limit) {
                // Allow request
                requests.add(now)
                RateLimitResult(
                    allowed = true,
                    limit = limit,
                    remaining = remaining,
                    resetAtMs = resetTime
                )
            } else {
                // Rate limit exceeded
                RateLimitResult(
                    allowed = false,
                    limit = limit,
                    remaining = 0,
                    resetAtMs = resetTime
                )
            }
        }
    }
    
    /**
     * Clean up old entries periodically (call from a background job)
     */
    fun cleanup() {
        val windowStart = System.currentTimeMillis() - RateLimitConfig.WINDOW_SIZE_MS
        requestWindows.forEach { (key, requests) ->
            synchronized(requests) {
                requests.removeIf { it < windowStart }
            }
        }
        // Remove empty entries
        requestWindows.entries.removeIf { it.value.isEmpty() }
    }
}

data class RateLimitResult(
    val allowed: Boolean,
    val limit: Int,
    val remaining: Int,
    val resetAtMs: Long
)

@Serializable
data class RateLimitErrorResponse(
    val status: String = "error",
    val code: Int = 429,
    val message: String,
    val error: RateLimitErrorDetail
)

@Serializable
data class RateLimitErrorDetail(
    val code: String = "RATE_LIMIT_EXCEEDED",
    val message: String,
    val type: String = "rate_limit_error",
    val retry_after_seconds: Int
)

/**
 * Rate limiting middleware for Ktor
 */
fun Application.rateLimit() {
    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()
        val method = call.request.httpMethod.value
        
        // Skip rate limiting for certain paths
        if (path.startsWith("/health") || path == "/") {
            // Still add headers but with higher limits
        }
        
        // Get client identifier (IP address or API key if authenticated)
        val clientId = call.request.header("Authorization")?.take(50) 
            ?: call.request.origin.remoteHost
            ?: "unknown"
        
        // Normalize path (remove trailing slash, extract base path for parameterized routes)
        val normalizedPath = normalizePath(path)
        val endpoint = "$method $normalizedPath"
        
        // Get limit for this endpoint
        val limit = RateLimitConfig.getLimitForEndpoint(method, normalizedPath)
        
        // Check rate limit
        val result = RateLimiter.checkLimit(clientId, endpoint, limit)
        
        // Add rate limit headers to ALL responses
        call.response.header("X-RateLimit-Limit", result.limit.toString())
        call.response.header("X-RateLimit-Remaining", result.remaining.toString())
        call.response.header("X-RateLimit-Reset", (result.resetAtMs / 1000).toString())
        
        if (!result.allowed) {
            val retryAfter = ((result.resetAtMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(1)
            
            call.response.header("Retry-After", retryAfter.toString())
            
            call.respond(
                HttpStatusCode.TooManyRequests,
                RateLimitErrorResponse(
                    message = "Rate limit exceeded. Please retry after $retryAfter seconds.",
                    error = RateLimitErrorDetail(
                        message = "You have exceeded the rate limit of $limit requests per minute for this endpoint.",
                        retry_after_seconds = retryAfter
                    )
                )
            )
            finish()
            return@intercept
        }
    }
    
    // Start cleanup job (runs every minute)
    // In production, use a proper scheduler
    Thread {
        while (true) {
            Thread.sleep(60_000)
            RateLimiter.cleanup()
        }
    }.apply {
        isDaemon = true
        start()
    }
}

/**
 * Normalize path for rate limiting (handle path parameters)
 */
private fun normalizePath(path: String): String {
    // Remove trailing slash
    val trimmed = path.trimEnd('/')
    
    // Replace UUIDs and numeric IDs with placeholders for consistent rate limiting
    return trimmed
        .replace(Regex("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"), "/{id}")
        .replace(Regex("/\\d+"), "/{id}")
}

