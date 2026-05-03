package com.vellum.api.routes

import com.vellum.api.domain.model.PushRequest
import com.vellum.api.domain.service.TransactionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transactionRoutes(transactionService: TransactionService) {
    authenticate("auth-jwt") {
        route("/transactions") {

            // Push local transactions to server
            post("/push") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<PushRequest>()
                if (request.transactions.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No transactions provided"))
                    return@post
                }

                val response = transactionService.push(userId, request.transactions)
                call.respond(HttpStatusCode.OK, response)
            }

            // Pull server transactions since timestamp
            get("/pull") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val since = call.request.queryParameters["lastSync"]?.toLongOrNull() ?: 0L
                val response = transactionService.pull(userId, since)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
