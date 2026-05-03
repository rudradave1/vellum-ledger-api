package com.vellum.api.routes

import com.vellum.api.data.dao.InsightDao
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)
@Serializable
data class TransactionSummaryDto(
    val id: String,
    val amount: Double,
    val type: String,
    val category: String,
    val note: String,
    val createdAt: Long
)

@Serializable
data class InsightRequest(val transactions: List<TransactionSummaryDto>)


fun Route.insightRoutes(insightDao: InsightDao) {
    authenticate("auth-jwt") {
        route("/insights") {
            post("/monthly") {
                val userId = call.principal<JWTPrincipal>()
                    ?.payload?.getClaim("userId")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                // 1. Rate limiting check (30 days)
                val lastInsight = insightDao.getLastInsight(userId)
                if (lastInsight != null) {
                    val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
                    if (System.currentTimeMillis() - lastInsight.requestedAt < thirtyDaysMillis) {
                        call.respond(HttpStatusCode.TooManyRequests, lastInsight.insight)
                        return@post
                    }
                }

                // 2. Read transactions from body
            
                val request = try {
                    call.receive<InsightRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid transaction list format")
                    return@post
                }
                val transactions = request.transactions

                if (transactions.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, "No transactions provided for analysis")
                    return@post
                }

                // 3. Prepare AI prompt
                val apiKey = System.getenv("OPENROUTER_API_KEY")
                if (apiKey.isNullOrBlank()) {
                    call.respond(HttpStatusCode.InternalServerError, "AI service not configured")
                    return@post
                }

                val transactionData = transactions.joinToString("\n") { 
                    "${it.type}: ${it.amount} in ${it.category} (${it.note ?: ""})" 
                }

                val systemPrompt = "You are a personal finance assistant. Analyze these transactions and give a 3-sentence monthly summary. Focus on total spending, biggest category, and one actionable suggestion. Be direct and specific with numbers."
                
                // 4. Call OpenRouter
                val response: OpenRouterResponse = try {
                    client.post("https://openrouter.ai/api/v1/chat/completions") {
                        header(HttpHeaders.Authorization, "Bearer $apiKey")
                        contentType(ContentType.Application.Json)
                        setBody(OpenRouterRequest(
                            model = "mistralai/mistral-7b-instruct",
                            messages = listOf(
                                Message("system", systemPrompt),
                                Message("user", "Here are my transactions:\n$transactionData")
                            )
                        ))
                    }.body()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadGateway, "Failed to connect to AI service: ${e.message}")
                    return@post
                }

                val insight = response.choices.firstOrNull()?.message?.content 
                    ?: "Could not generate insight."

                // 5. Save and Return
                insightDao.saveInsight(userId, insight)
                call.respondText(insight, status = HttpStatusCode.OK)
            }
        }
    }
}
