package com.vellum.api.plugins

import com.vellum.api.data.dao.InsightDao
import com.vellum.api.data.dao.TransactionDao
import com.vellum.api.data.dao.UserDao
import com.vellum.api.domain.service.AuthService
import com.vellum.api.domain.service.TransactionService
import com.vellum.api.routes.authRoutes
import com.vellum.api.routes.insightRoutes
import com.vellum.api.routes.transactionRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "vellum-ledger"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "vellum-ledger-users"

    val userDao = UserDao()
    val transactionDao = TransactionDao()
    val insightDao = InsightDao()
    val authService = AuthService(userDao, jwtSecret, jwtIssuer, jwtAudience)
    val transactionService = TransactionService(transactionDao)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "vellum-ledger-api"))
        }
        authRoutes(authService)
        transactionRoutes(transactionService)
        insightRoutes(insightDao)
    }
}
