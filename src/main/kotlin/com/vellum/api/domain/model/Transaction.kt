package com.vellum.api.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,           // client-generated UUID
    val userId: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
    val type: String,         // INCOME or EXPENSE
    val cardId: String? = null,
    val createdAt: Long,      // epoch millis
    val updatedAt: Long,      // conflict resolution key
    val syncStatus: String = "SYNCED"
)
