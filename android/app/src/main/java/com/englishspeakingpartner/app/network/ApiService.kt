package com.englishspeakingpartner.app.network

import com.englishspeakingpartner.app.data.ChatRequest
import com.englishspeakingpartner.app.data.ChatResponse
import com.englishspeakingpartner.app.data.TtsRequest
import com.englishspeakingpartner.app.data.TtsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @POST("api/tts")
    suspend fun ttsJson(@Body request: TtsRequest): Response<TtsResponse>

    @POST("api/tts")
    suspend fun ttsAudio(@Body request: TtsRequest): Response<ResponseBody>

    @GET("health")
    suspend fun health(): Response<Map<String, String>>
}
