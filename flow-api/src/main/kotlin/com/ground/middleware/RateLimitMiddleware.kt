package com.ground.middleware

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
 * 
 * SECURITY: Auth endpoints have stricter limits to prevent brute force attacks.
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
        // ============================================
        // SECURITY: Auth endpoints - STRICT LIMITS
        // These are critical for preventing brute force
        // ============================================
        "POST /v1/auth/authenticate" to 5,    // 5 login attempts per minute per IP
        "POST /v1/auth/refresh" to 20,        // 20 token refreshes per minute
        "POST /v1/accounts" to 3,             // 3 signups per minute (spam prevention)
        
        // Account settings - sensitive operations
        "POST /v1/account/password" to 3,     // 3 password change attempts per minute
        "PATCH /v1/account/email" to 3,       // 3 email change attempts per minute
        "DELETE /v1/account" to 1,            // 1 account deletion per minute
        "DELETE /v1/sessions/{id}" to 10,     // 10 session revocations per minute
        "DELETE /v1/sessions" to 2,           // 2 "revoke all" per minute
        
        // ============================================
        // Standard API endpoints (75-100 req/min)
        // ============================================
        "GET /v1/markets" to 75,
        "GET /v1/yield/rates" to 75,
        "GET /v1/yield/accounts" to 100,
        "POST /v1/yield/accounts" to 100,
        "GET /v1/yield/positions" to 100,
        "GET /v1/transactions" to 100,
        "POST /v1/applications" to 100,
        "GET /v1/applications" to 100,
        
        // Webhooks
        "POST /v1/webhooks" to 20,            // 20 webhook creations per minute
        "GET /v1/webhooks" to 60,             // 60 webhook list requests per minute
        "POST /v1/webhooks/{id}/test" to 10,  // 10 test webhook sends per minute
        
        // ============================================
        // Health/Status - Higher limits
        // ============================================
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
        
        // Get client identifier (IP address)
        // For auth endpoints, we ALWAYS use IP (not API key) to prevent brute force
        val clientId = if (isAuthEndpoint(path)) {
            // Auth endpoints: always use IP for rate limiting
            getClientIp(call)
        } else {
            // Other endpoints: use API key if available, else IP
            call.request.header("Authorization")?.take(50) 
                ?: getClientIp(call)
        }
        
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
 * Check if this is an auth-related endpoint (always rate limit by IP)
 */
private fun isAuthEndpoint(path: String): Boolean {
    return path.startsWith("/v1/auth") || 
           path.startsWith("/v1/accounts") ||
           path.startsWith("/v1/account") ||
           path.startsWith("/v1/sessions")
}

/**
 * Get the real client IP address, handling proxies
 */
private fun getClientIp(call: ApplicationCall): String {
    // Check X-Forwarded-For (when behind a proxy/load balancer)
    val forwarded = call.request.header("X-Forwarded-For")
    if (forwarded != null) {
        // Take the first IP (original client)
        return forwarded.split(",").firstOrNull()?.trim() ?: "unknown"
    }
    
    // Check X-Real-IP
    val realIp = call.request.header("X-Real-IP")
    if (realIp != null) {
        return realIp.trim()
    }
    
    // Fall back to direct connection IP
    return call.request.origin.remoteHost ?: "unknown"
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
