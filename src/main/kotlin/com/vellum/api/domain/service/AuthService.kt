package com.vellum.api.domain.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.vellum.api.data.dao.UserDao
import com.vellum.api.domain.model.AuthResponse
import java.util.Date

class AuthService(
    private val userDao: UserDao,
    private val jwtSecret: String,
    private val jwtIssuer: String,
    private val jwtAudience: String
) {

    fun register(email: String, password: String): AuthResponse {
        val existing = userDao.findByEmail(email)
        if (existing != null) error("Email already registered")

        val hash = BCrypt.withDefaults().hashToString(12, password.toCharArray())
        val user = userDao.create(email, hash)
        val token = generateToken(user.id)

        return AuthResponse(token = token, userId = user.id, email = user.email)
    }

    fun login(email: String, password: String): AuthResponse {
        val (user, hash) = userDao.findByEmail(email)
            ?: error("Invalid credentials")

        val verified = BCrypt.verifyer().verify(password.toCharArray(), hash)
        if (!verified.verified) error("Invalid credentials")

        val token = generateToken(user.id)
        return AuthResponse(token = token, userId = user.id, email = user.email)
    }

    private fun generateToken(userId: String): String {
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) // 30 days
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}
