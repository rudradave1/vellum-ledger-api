package com.vellum.api.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PushRequest(
    val transactions: List<Transaction>
)

@Serializable
data class PushResponse(
    val synced: Int,
    val conflicts: Int,
    val failed: Int
)

@Serializable
data class PullResponse(
    val transactions: List<Transaction>,
    val serverTime: Long
)

@Serializable
data class StatusResponse(
    val id: String,
    val syncStatus: String,
    val updatedAt: Long
)
