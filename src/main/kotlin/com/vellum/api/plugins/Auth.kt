package com.vellum.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "vellum-ledger"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "vellum-ledger-users"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Vellum Ledger API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (userId != null) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
