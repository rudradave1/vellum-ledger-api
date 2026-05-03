package com.vellum.api.data.dao

import com.vellum.api.data.tables.TransactionsTable
import com.vellum.api.domain.model.Transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class TransactionDao {

    fun upsert(incoming: Transaction): String {
        return transaction {
            val existing = TransactionsTable
                .select { 
                    (TransactionsTable.id eq incoming.id) and 
                    (TransactionsTable.userId eq incoming.userId) 
                }
                .singleOrNull()

            when {
                existing == null -> {
                    TransactionsTable.insert { row ->
                        row[id] = incoming.id
                        row[userId] = incoming.userId
                        row[amount] = incoming.amount
                        row[category] = incoming.category
                        row[note] = incoming.note
                        row[type] = incoming.type
                        row[cardId] = incoming.cardId
                        row[createdAt] = incoming.createdAt
                        row[updatedAt] = incoming.updatedAt
                        row[syncStatus] = "SYNCED"
                    }
                    "SYNCED"
                }
                incoming.updatedAt > existing[TransactionsTable.updatedAt] -> {
                    // Incoming wins — timestamp-based conflict resolution
                    TransactionsTable.update({
                        (TransactionsTable.id eq incoming.id) and
                        (TransactionsTable.userId eq incoming.userId)
                    }) { row ->
                        row[amount] = incoming.amount
                        row[category] = incoming.category
                        row[note] = incoming.note
                        row[type] = incoming.type
                        row[cardId] = incoming.cardId
                        row[updatedAt] = incoming.updatedAt
                        row[syncStatus] = "SYNCED"
                    }
                    "SYNCED"
                }
                else -> "CONFLICT" // Server version is newer, reject
            }
        }
    }

    fun getForUser(userId: String, since: Long): List<Transaction> = transaction {
        TransactionsTable
            .select { 
                (TransactionsTable.userId eq userId) and 
                (TransactionsTable.updatedAt greaterEq since) 
            }
            .orderBy(TransactionsTable.updatedAt, SortOrder.DESC)
            .map { row -> row.toTransaction() }
    }

    private fun ResultRow.toTransaction() = Transaction(
        id = this[TransactionsTable.id],
        userId = this[TransactionsTable.userId],
        amount = this[TransactionsTable.amount],
        category = this[TransactionsTable.category],
        note = this[TransactionsTable.note],
        type = this[TransactionsTable.type],
        cardId = this[TransactionsTable.cardId],
        createdAt = this[TransactionsTable.createdAt],
        updatedAt = this[TransactionsTable.updatedAt],
        syncStatus = this[TransactionsTable.syncStatus]
    )
}
