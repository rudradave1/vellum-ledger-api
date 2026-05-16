package com.vellum.api.routes

import com.vellum.api.data.dao.InsightDao
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
    val choices: List<Choice>? = null
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
    val note: String? = null,
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
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

                if (lastInsight != null && lastInsight.requestedAt > thirtyDaysAgo) {
                    println("Rate limit hit: Returning cached insight for $userId")
                    call.respond(HttpStatusCode.TooManyRequests, lastInsight.insight)
                    return@post
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
                    val amountInMajorUnits = it.amount / 100.0
                    "${it.type}: ₹${"%.2f".format(amountInMajorUnits)} in ${it.category} (${it.note ?: ""})"
                }

                val systemPrompt = "You are a personal finance assistant. Analyze these transactions and give a 3-sentence monthly summary. Focus on total spending, biggest category, and one actionable suggestion. Be direct and specific with numbers."
                
                // 4. Call OpenRouter
                val openRouterHttpResponse = client.post("https://openrouter.ai/api/v1/chat/completions") {
                    header("Authorization", "Bearer ${System.getenv("OPENROUTER_API_KEY")}")
                    header("Content-Type", "application/json")
                    setBody(OpenRouterRequest(
                        model = "openrouter/free",
                        messages = listOf(
                            Message("system", systemPrompt),
                            Message("user", "Here are my transactions:\n$transactionData")
                        )
                    ))
                }

                val responseBody = openRouterHttpResponse.bodyAsText()
                println("OpenRouter raw response: $responseBody")

                if (openRouterHttpResponse.status.value != 200) {
                    println("OpenRouter error status: ${openRouterHttpResponse.status.value}")
                    call.respond(
                        HttpStatusCode.BadGateway,
                        "AI service error: ${openRouterHttpResponse.status.value}"
                    )
                    return@post
                }

                val parsed = try {
                    Json { ignoreUnknownKeys = true }.decodeFromString<OpenRouterResponse>(responseBody)
                } catch (e: Exception) {
                    println("OpenRouter parse error: ${e.message}")
                    call.respond(HttpStatusCode.BadGateway, "Failed to parse AI response")
                    return@post
                }

                val insight = parsed.choices?.firstOrNull()?.message?.content
                    ?: run {
                        call.respond(HttpStatusCode.BadGateway, "Empty AI response")
                        return@post
                    }

                // 5. Save and Return
                insightDao.saveInsight(userId, insight)
                call.respondText(insight, status = HttpStatusCode.OK)
            }
        }
    }
}
