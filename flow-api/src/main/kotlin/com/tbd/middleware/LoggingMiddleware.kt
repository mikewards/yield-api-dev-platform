package com.tbd.middleware

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import org.slf4j.event.Level

fun Application.logging() {
    install(CallLogging) {
        level = Level.INFO
    }
}
