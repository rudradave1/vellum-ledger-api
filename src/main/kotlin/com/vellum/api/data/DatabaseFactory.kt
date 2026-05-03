package com.vellum.api.data

import com.vellum.api.data.tables.TransactionsTable
import com.vellum.api.data.tables.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun init() {
        val database = try {
            Database.connect(createHikariDataSource())
        } catch (e: Exception) {
            logger.warn("Failed to connect to PostgreSQL, falling back to H2 in-memory database: ${e.message}")
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        }

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                TransactionsTable
            )
        }
    }

    private fun createHikariDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("DATABASE_URL")
                ?: "jdbc:postgresql://localhost:5432/vellum_ledger"
            username = System.getenv("DATABASE_USER") ?: "postgres"
            password = System.getenv("DATABASE_PASSWORD") ?: "password"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            // Add a short timeout for local fallback
            connectionTimeout = 2000 
            validate()
        }
        return HikariDataSource(config)
    }
}
