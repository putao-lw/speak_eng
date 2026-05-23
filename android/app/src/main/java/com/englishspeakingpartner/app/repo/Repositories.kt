package com.englishspeakingpartner.app.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.englishspeakingpartner.app.data.ChatRequest
import com.englishspeakingpartner.app.data.ChatResponse
import com.englishspeakingpartner.app.data.PracticeHistory
import com.englishspeakingpartner.app.data.TtsRequest
import com.englishspeakingpartner.app.data.TtsResponse
import com.englishspeakingpartner.app.data.UserSettings
import com.englishspeakingpartner.app.db.PracticeHistoryDao
import com.englishspeakingpartner.app.db.PracticeHistoryEntity
import com.englishspeakingpartner.app.network.ModelRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val backend = stringPreferencesKey("backend")
        val voice = stringPreferencesKey("voice")
        val speed = stringPreferencesKey("speed")
        val autoPlay = booleanPreferencesKey("auto_play")
        val showChinese = booleanPreferencesKey("show_chinese")
        val saveHistory = booleanPreferencesKey("save_history")
    }

    val settings: Flow<UserSettings> = context.settingsStore.data.map { prefs ->
        UserSettings(
            backendBaseUrl = prefs[Keys.backend] ?: "http://10.0.2.2:8000/",
            voice = prefs[Keys.voice] ?: "default_female",
            speed = prefs[Keys.speed] ?: "normal",
            autoPlay = prefs[Keys.autoPlay] ?: true,
            showChinese = prefs[Keys.showChinese] ?: true,
            saveHistory = prefs[Keys.saveHistory] ?: true
        )
    }

    suspend fun update(settings: UserSettings) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.backend] = settings.backendBaseUrl
            prefs[Keys.voice] = settings.voice
            prefs[Keys.speed] = settings.speed
            prefs[Keys.autoPlay] = settings.autoPlay
            prefs[Keys.showChinese] = settings.showChinese
            prefs[Keys.saveHistory] = settings.saveHistory
        }
    }
}

class HistoryRepository(private val dao: PracticeHistoryDao) {
    val history: Flow<List<PracticeHistory>> = dao.observeAll().map { rows ->
        rows.map {
            PracticeHistory(
                id = it.id,
                sessionId = it.sessionId,
                timestamp = it.timestamp,
                level = it.level,
                topic = it.topic,
                userOriginal = it.userOriginal,
                userTranslationZh = it.userTranslationZh,
                scoreTotal = it.scoreTotal,
                problemsJson = it.problemsJson,
                betterExpressionsJson = it.betterExpressionsJson,
                aiReplyEnglish = it.aiReplyEnglish,
                aiReplyChinese = it.aiReplyChinese
            )
        }
    }

    suspend fun add(entity: PracticeHistoryEntity) = dao.insert(entity)
    suspend fun clear() = dao.clear()
}

class PracticeRepository(
    private val remote: ModelRemoteDataSource,
    private val historyRepository: HistoryRepository
) {
    suspend fun send(baseUrl: String, request: ChatRequest): ChatResponse {
        if (baseUrl.isBlank()) error("请先在设置中填写后端 API 地址")
        return remote.chat(baseUrl, request)
    }

    suspend fun tts(baseUrl: String, request: TtsRequest): TtsResponse {
        if (baseUrl.isBlank()) error("请先在设置中填写后端 API 地址")
        return remote.tts(baseUrl, request)
    }

    suspend fun saveHistory(entity: PracticeHistoryEntity) {
        historyRepository.add(entity)
    }
}
