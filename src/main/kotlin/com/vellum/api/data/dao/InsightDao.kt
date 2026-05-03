package com.vellum.api.data.dao

import com.vellum.api.data.tables.InsightRequestsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class InsightDao {

    data class InsightResult(val insight: String, val requestedAt: Long)

    fun getLastInsight(userId: String): InsightResult? = transaction {
        InsightRequestsTable
            .select { InsightRequestsTable.userId eq userId }
            .map { 
                InsightResult(
                    insight = it[InsightRequestsTable.lastInsight],
                    requestedAt = it[InsightRequestsTable.lastRequestedAt]
                )
            }
            .singleOrNull()
    }

    fun saveInsight(userId: String, insight: String) = transaction {
        val existing = InsightRequestsTable
            .select { InsightRequestsTable.userId eq userId }
            .singleOrNull()

        if (existing == null) {
            InsightRequestsTable.insert {
                it[InsightRequestsTable.userId] = userId
                it[InsightRequestsTable.lastInsight] = insight
                it[InsightRequestsTable.lastRequestedAt] = System.currentTimeMillis()
            }
        } else {
            InsightRequestsTable.update({ InsightRequestsTable.userId eq userId }) {
                it[InsightRequestsTable.lastInsight] = insight
                it[InsightRequestsTable.lastRequestedAt] = System.currentTimeMillis()
            }
        }
    }
}
