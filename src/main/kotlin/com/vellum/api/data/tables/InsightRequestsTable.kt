package com.vellum.api.data.tables

import org.jetbrains.exposed.sql.Table

object InsightRequestsTable : Table("insight_requests") {
    val userId = varchar("user_id", 36)
    val lastRequestedAt = long("last_requested_at")
    val lastInsight = text("last_insight")

    override val primaryKey = PrimaryKey(userId)
}
