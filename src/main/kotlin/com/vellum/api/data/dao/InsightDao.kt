package com.vellum.api.data.dao

import com.vellum.api.data.tables.InsightRequestsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class InsightDao {

    data class InsightResult(val insight: String, val requestedAt: Long)

    fun getLastInsight(userId: String): InsightResult? = transaction {
        println("Checking cache for userId: $userId")
        InsightRequestsTable
            .selectAll()
            .where { InsightRequestsTable.userId eq userId }
            .map { 
                InsightResult(
                    insight = it[InsightRequestsTable.lastInsight],
                    requestedAt = it[InsightRequestsTable.lastRequestedAt]
                )
            }
            .singleOrNull()
            .also {
                if (it != null) println("Found cached insight from: ${it.requestedAt}")
                else println("No cached insight found for user")
            }
    }

    fun saveInsight(userId: String, insight: String) = transaction {
        println("Saving insight for userId: $userId")
        val existing = InsightRequestsTable
            .selectAll()
            .where { InsightRequestsTable.userId eq userId }
            .singleOrNull()

        if (existing == null) {
            println("Inserting new insight record")
            InsightRequestsTable.insert {
                it[InsightRequestsTable.userId] = userId
                it[InsightRequestsTable.lastInsight] = insight
                it[InsightRequestsTable.lastRequestedAt] = System.currentTimeMillis()
            }
        } else {
            println("Updating existing insight record")
            InsightRequestsTable.update({ InsightRequestsTable.userId eq userId }) {
                it[InsightRequestsTable.lastInsight] = insight
                it[InsightRequestsTable.lastRequestedAt] = System.currentTimeMillis()
            }
        }
    }
}
