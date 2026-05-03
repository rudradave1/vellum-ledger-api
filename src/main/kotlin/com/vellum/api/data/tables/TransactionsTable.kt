package com.vellum.api.data.tables

import org.jetbrains.exposed.sql.Table

object TransactionsTable : Table("transactions") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).references(UsersTable.id)
    val amount = double("amount")
    val category = varchar("category", 100)
    val note = varchar("note", 500).nullable()
    val type = varchar("type", 10)
    val cardId = varchar("card_id", 36).nullable()
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val syncStatus = varchar("sync_status", 20).default("SYNCED")

    override val primaryKey = PrimaryKey(id)
}
