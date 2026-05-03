package com.vellum.api.domain.service

import com.vellum.api.data.dao.TransactionDao
import com.vellum.api.domain.model.*

class TransactionService(private val dao: TransactionDao) {

    fun push(userId: String, transactions: List<Transaction>): PushResponse {
        var synced = 0
        var conflicts = 0
        var failed = 0

        transactions.forEach { transaction ->
            if (transaction.userId != userId) {
                failed++
                return@forEach
            }
            try {
                when (dao.upsert(transaction)) {
                    "SYNCED" -> synced++
                    "CONFLICT" -> conflicts++
                    else -> failed++
                }
            } catch (e: Exception) {
                failed++
            }
        }

        return PushResponse(synced = synced, conflicts = conflicts, failed = failed)
    }

    fun pull(userId: String, since: Long): PullResponse {
        val transactions = dao.getForUser(userId, since)
        return PullResponse(
            transactions = transactions,
            serverTime = System.currentTimeMillis()
        )
    }
}
