package com.flow.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.transactionRoutes() {
    routing {
        route("/v1/transactions") {
            authenticate("bearer-auth") {
                get {
                    // TODO: Implement transaction listing
                    call.respond(HttpStatusCode.OK, emptyList<Any>())
                }
            }
        }
    }
}