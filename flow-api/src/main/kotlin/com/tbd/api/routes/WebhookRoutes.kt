package com.tbd.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.webhookRoutes() {
    routing {
        route("/v1/webhooks") {
            authenticate("bearer-auth") {
                get {
                    // TODO: Implement webhook listing
                    call.respond(HttpStatusCode.OK, emptyList<Any>())
                }
            }
        }
    }
}