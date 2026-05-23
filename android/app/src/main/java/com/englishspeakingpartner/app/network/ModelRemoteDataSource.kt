package com.englishspeakingpartner.app.network

import com.englishspeakingpartner.app.data.ChatRequest
import com.englishspeakingpartner.app.data.ChatResponse
import com.englishspeakingpartner.app.data.TtsRequest
import com.englishspeakingpartner.app.data.TtsResponse

class ModelRemoteDataSource {
    suspend fun chat(baseUrl: String, request: ChatRequest): ChatResponse {
        val response = ApiClient.service(baseUrl).chat(request)
        if (!response.isSuccessful) error("后端返回失败：${response.code()}")
        return response.body() ?: error("后端返回内容为空")
    }

    suspend fun tts(baseUrl: String, request: TtsRequest): TtsResponse {
        val response = ApiClient.service(baseUrl).ttsJson(request)
        if (!response.isSuccessful) error("TTS 返回失败：${response.code()}")
        return response.body() ?: TtsResponse()
    }
}
