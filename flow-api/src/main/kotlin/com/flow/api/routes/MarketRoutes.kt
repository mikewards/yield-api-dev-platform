package com.flow.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.marketRoutes() {
    routing {
        route("/v1/markets") {
            get {
                // TODO: Implement market listing
                call.respond(HttpStatusCode.OK, emptyList<Any>())
            }
        }
    }
}
