package com.vellum.api.data.dao

import com.vellum.api.data.tables.UsersTable
import com.vellum.api.domain.model.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class UserDao {

    fun create(email: String, passwordHash: String): User = transaction {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.createdAt] = now
        }
        User(id = id, email = email, createdAt = now)
    }

    fun findByEmail(email: String): Pair<User, String>? = transaction {
        UsersTable
            .select { UsersTable.email eq email }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[UsersTable.id],
                    email = row[UsersTable.email],
                    createdAt = row[UsersTable.createdAt]
                ) to row[UsersTable.passwordHash]
            }
    }

    fun findById(id: String): User? = transaction {
        UsersTable
            .select { UsersTable.id eq id }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[UsersTable.id],
                    email = row[UsersTable.email],
                    createdAt = row[UsersTable.createdAt]
                )
            }
    }
}
