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
        val rawUrl = System.getenv("DATABASE_URL") ?: throw IllegalStateException("DATABASE_URL not set")
        
        println("Raw DATABASE_URL: $rawUrl")
        
        // Convert postgres:// or postgresql:// to jdbc:postgresql://
        val jdbcUrl = rawUrl
            .replace("^postgres://".toRegex(), "jdbc:postgresql://")
            .replace("^postgresql://".toRegex(), "jdbc:postgresql://")
        
        println("JDBC URL: $jdbcUrl")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            setJdbcUrl(jdbcUrl)
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}
