package com.englishspeakingpartner.app.data

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("session_id") val sessionId: String,
    val level: String,
    val topic: String,
    @SerializedName("user_input") val userInput: String,
    @SerializedName("input_type") val inputType: String,
    val history: List<ChatMessage> = emptyList()
)

data class ChatMessage(
    val role: String,
    val english: String,
    val chinese: String
)

data class ChatResponse(
    @SerializedName("user_original") val userOriginal: String,
    @SerializedName("user_translation_zh") val userTranslationZh: String,
    val score: Score,
    val problems: List<Problem>,
    @SerializedName("better_expressions") val betterExpressions: List<BetterExpression>,
    @SerializedName("ai_reply") val aiReply: AiReply,
    @SerializedName("encouragement_zh") val encouragementZh: String
)

data class Score(
    val total: Int,
    val naturalness: Int,
    val clarity: Int,
    val grammar: Int,
    val vocabulary: Int,
    @SerializedName("level_match") val levelMatch: Int
)

data class Problem(
    val type: String,
    @SerializedName("original_part") val originalPart: String,
    @SerializedName("explanation_zh") val explanationZh: String
)

data class BetterExpression(
    val english: String,
    val chinese: String,
    @SerializedName("why_zh") val whyZh: String
)

data class AiReply(
    val english: String,
    val chinese: String
)

data class TtsRequest(
    val text: String,
    val voice: String,
    val speed: String
)

data class TtsResponse(
    @SerializedName("audio_url") val audioUrl: String? = null,
    @SerializedName("audio_base64") val audioBase64: String? = null,
    @SerializedName("mime_type") val mimeType: String? = null
)

data class PracticeHistory(
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val level: String,
    val topic: String,
    val userOriginal: String,
    val userTranslationZh: String,
    val scoreTotal: Int,
    val problemsJson: String,
    val betterExpressionsJson: String,
    val aiReplyEnglish: String,
    val aiReplyChinese: String
)

data class UserSettings(
    val backendBaseUrl: String = "http://10.0.2.2:8000/",
    val voice: String = "default_female",
    val speed: String = "normal",
    val autoPlay: Boolean = true,
    val showChinese: Boolean = true,
    val saveHistory: Boolean = true
)
