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
        println("Starting DatabaseFactory.init()")
        println("DATABASE_URL: ${System.getenv("DATABASE_URL")}")
        
        val rawUrl = System.getenv("DATABASE_URL") 
            ?: throw IllegalStateException("DATABASE_URL is not set")
        
        val jdbcUrl = rawUrl
            .replace("^postgres://".toRegex(), "jdbc:postgresql://")
            .replace("^postgresql://".toRegex(), "jdbc:postgresql://")
        
        println("Converted JDBC URL: $jdbcUrl")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            setJdbcUrl(jdbcUrl)
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        println("Connecting to PostgreSQL...")
        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)
        println("Connected successfully")

        transaction(database) {
            println("Running migrations...")
            SchemaUtils.create(
                UsersTable,
                TransactionsTable,
                InsightRequestsTable
            )
            println("Migrations complete")
        }
        
        println("DatabaseFactory.init() finished")
    }
}