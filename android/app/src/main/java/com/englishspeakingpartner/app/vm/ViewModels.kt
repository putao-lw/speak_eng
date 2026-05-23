package com.englishspeakingpartner.app.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.englishspeakingpartner.app.AppContainer
import com.englishspeakingpartner.app.data.AiReply
import com.englishspeakingpartner.app.data.ChatMessage
import com.englishspeakingpartner.app.data.ChatRequest
import com.englishspeakingpartner.app.data.ChatResponse
import com.englishspeakingpartner.app.data.TtsRequest
import com.englishspeakingpartner.app.data.UserSettings
import com.englishspeakingpartner.app.db.PracticeHistoryEntity
import com.englishspeakingpartner.app.repo.HistoryRepository
import com.englishspeakingpartner.app.repo.PracticeRepository
import com.englishspeakingpartner.app.repo.SettingsRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PracticeUiState(
    val sessionId: String = UUID.randomUUID().toString(),
    val level: String = "junior",
    val topic: String = "Daily Life 日常生活",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0,
    val lastResponse: ChatResponse? = null,
    val aiReply: AiReply = AiReply(
        english = "Hi! I am your English speaking partner. What would you like to talk about today?",
        chinese = "你好！我是你的英语口语搭子。今天你想聊什么？"
    ),
    val message: String? = null,
    val history: List<ChatMessage> = emptyList()
)

class PracticeViewModel(
    private val practiceRepository: PracticeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val gson = Gson()
    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun start(level: String, topic: String) {
        _uiState.update { it.copy(level = level, topic = topic, elapsedSeconds = 0) }
    }

    fun setInput(text: String) = _uiState.update { it.copy(inputText = text) }
    fun setListening(listening: Boolean) = _uiState.update { it.copy(isListening = listening) }
    fun setPaused(paused: Boolean) = _uiState.update { it.copy(isPaused = paused) }
    fun tick() = _uiState.update { if (it.isPaused) it else it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun showMessage(message: String) = _uiState.update { it.copy(message = message) }

    fun send(inputType: String = "text", onTtsReady: (String?, String?, String?) -> Unit = { _, _, _ -> }) {
        val current = _uiState.value
        val text = current.inputText.trim()
        if (text.isBlank()) {
            showMessage("请先输入或说一句英文")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching {
                val request = ChatRequest(
                    sessionId = current.sessionId,
                    level = current.level,
                    topic = current.topic,
                    userInput = text,
                    inputType = inputType,
                    history = current.history.takeLast(10)
                )
                val response = practiceRepository.send(settings.value.backendBaseUrl, request)
                val nextHistory = current.history + listOf(
                    ChatMessage("user", response.userOriginal, response.userTranslationZh),
                    ChatMessage("ai", response.aiReply.english, response.aiReply.chinese)
                )
                _uiState.update {
                    it.copy(
                        inputText = "",
                        isLoading = false,
                        lastResponse = response,
                        aiReply = response.aiReply,
                        history = nextHistory
                    )
                }

                if (settings.value.saveHistory) {
                    // 保存结构化结果，详情页可稳定还原问题和地道表达。
                    practiceRepository.saveHistory(
                        PracticeHistoryEntity(
                            sessionId = current.sessionId,
                            timestamp = System.currentTimeMillis(),
                            level = current.level,
                            topic = current.topic,
                            userOriginal = response.userOriginal,
                            userTranslationZh = response.userTranslationZh,
                            scoreTotal = response.score.total,
                            problemsJson = gson.toJson(response.problems),
                            betterExpressionsJson = gson.toJson(response.betterExpressions),
                            aiReplyEnglish = response.aiReply.english,
                            aiReplyChinese = response.aiReply.chinese
                        )
                    )
                }

                if (settings.value.autoPlay) {
                    val tts = practiceRepository.tts(
                        settings.value.backendBaseUrl,
                        TtsRequest(response.aiReply.english, settings.value.voice, settings.value.speed)
                    )
                    onTtsReady(tts.audioUrl, tts.audioBase64, tts.mimeType)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, message = error.message ?: "请求失败") }
            }
        }
    }

    fun replay(onTtsReady: (String?, String?, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val tts = practiceRepository.tts(
                    settings.value.backendBaseUrl,
                    TtsRequest(_uiState.value.aiReply.english, settings.value.voice, settings.value.speed)
                )
                onTtsReady(tts.audioUrl, tts.audioBase64, tts.mimeType)
            }.onFailure { showMessage(it.message ?: "TTS 播放失败") }
        }
    }
}

class HistoryViewModel(private val historyRepository: HistoryRepository) : ViewModel() {
    val history = historyRepository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        viewModelScope.launch { historyRepository.clear() }
    }
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun update(settings: UserSettings) {
        viewModelScope.launch { settingsRepository.update(settings) }
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            PracticeViewModel::class.java -> PracticeViewModel(container.practiceRepository, container.settingsRepository) as T
            HistoryViewModel::class.java -> HistoryViewModel(container.historyRepository) as T
            SettingsViewModel::class.java -> SettingsViewModel(container.settingsRepository) as T
            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
