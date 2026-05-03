package com.vellum.api.data

import com.vellum.api.data.tables.InsightRequestsTable
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
        println("DATABASE_URL raw value: ${System.getenv("DATABASE_URL")}")

        val database = try {
            Database.connect(createHikariDataSource())
        } catch (e: Exception) {
            logger.warn("Failed to connect to PostgreSQL, falling back to H2 in-memory database: ${e.message}")
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        }

        transaction(database) {
            println("Running database migrations...")
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                TransactionsTable,
                InsightRequestsTable
            )
            println("Database migrations completed.")
        }
    }

    private fun createHikariDataSource(): HikariDataSource {
        val rawUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/vellum_ledger"
        
        val jdbcUrl = if (rawUrl.startsWith("postgresql://")) {
            rawUrl.replace("postgresql://", "jdbc:postgresql://")
        } else {
            rawUrl
        }
        
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            setJdbcUrl(jdbcUrl)
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            connectionTimeout = 2000
            validate()
        }
        return HikariDataSource(config)
    }
}
