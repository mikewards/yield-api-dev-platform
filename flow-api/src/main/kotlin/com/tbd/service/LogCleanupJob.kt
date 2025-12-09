package com.tbd.service

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Background job to clean up logs older than 7 days.
 * Runs every 6 hours to keep database size manageable.
 */
object LogCleanupJob {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logService = RequestLogService()
    private var isRunning = false
    
    fun start() {
        if (isRunning) return
        isRunning = true
        
        scope.launch {
            while (isActive) {
                try {
                    val deleted = logService.cleanupOldLogs()
                    if (deleted > 0) {
                        println("Log cleanup: Deleted $deleted logs older than 7 days")
                    }
                } catch (e: Exception) {
                    println("Log cleanup failed: ${e.message}")
                }
                
                // Wait 6 hours before next cleanup
                delay(TimeUnit.HOURS.toMillis(6))
            }
        }
        
        println("Log cleanup job started (runs every 6 hours)")
    }
    
    fun stop() {
        isRunning = false
        scope.cancel()
    }
}

