package com.vellum.api.routes

import com.vellum.api.domain.model.LoginRequest
import com.vellum.api.domain.model.RegisterRequest
import com.vellum.api.domain.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            if (request.email.isBlank() || request.password.length < 8) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid email or password too short")
                )
                return@post
            }
            try {
                val response = authService.register(request.email, request.password)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to (e.message ?: "Registration failed"))
                )
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            try {
                val response = authService.login(request.email, request.password)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("error" to "Invalid credentials")
                )
            }
        }
    }
}
