package com.tbd.util

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Retry configuration
 */
data class RetryConfig(
    val maxAttempts: Int = 2,
    val initialDelay: kotlin.time.Duration = 100.milliseconds,
    val maxDelay: kotlin.time.Duration = 1.seconds,
    val multiplier: Double = 2.0,
    val retryableExceptions: Set<Class<out Throwable>> = setOf(
        java.net.ConnectException::class.java,
        java.net.SocketTimeoutException::class.java,
        java.io.IOException::class.java,
        java.util.concurrent.TimeoutException::class.java
    )
)

/**
 * Execute a suspend function with exponential backoff retry logic
 */
suspend fun <T> retryWithBackoff(
    config: RetryConfig = RetryConfig(),
    block: suspend () -> T
): T {
    var lastException: Throwable? = null
    var currentDelay = config.initialDelay
    
    repeat(config.maxAttempts) { attempt ->
        try {
            return block()
        } catch (e: Throwable) {
            lastException = e
            
            // Check if exception is retryable
            val isRetryable = config.retryableExceptions.any { it.isInstance(e) }
            if (!isRetryable || attempt == config.maxAttempts - 1) {
                throw e
            }
            
            // Log retry attempt
            println("⚠️  Retry attempt ${attempt + 1}/${config.maxAttempts} after ${currentDelay.inWholeMilliseconds}ms: ${e.javaClass.simpleName}")
            
            // Wait before retry (exponential backoff)
            delay(currentDelay)
            currentDelay = (currentDelay * config.multiplier).coerceAtMost(config.maxDelay)
        }
    }
    
    throw lastException ?: Exception("Retry failed")
}

