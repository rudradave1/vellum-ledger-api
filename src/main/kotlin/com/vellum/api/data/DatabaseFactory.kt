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
        
        val dataSource = createHikariDataSource()
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

    private fun createHikariDataSource(): HikariDataSource {
        val rawUrl = System.getenv("DATABASE_URL")
            ?: throw IllegalStateException("DATABASE_URL not set")

        println("Raw URL: $rawUrl")

        val uri = java.net.URI(rawUrl)
        val host = uri.host
        val port = uri.port
        val dbName = uri.path.removePrefix("/")
        val userInfo = uri.userInfo ?: ":"
        val (user, password) = userInfo.split(":")

        val jdbcUrl = if (port != -1) {
            "jdbc:postgresql://$host:$port/$dbName"
        } else {
            "jdbc:postgresql://$host/$dbName"
        }

        println("JDBC URL: $jdbcUrl")
        println("User: $user")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            setJdbcUrl(jdbcUrl)
            username = user
            setPassword(password)
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }

        return HikariDataSource(config)
    }
}