package com.englishspeakingpartner.app.speech

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File

class TtsPlayerManager(private val context: Context) {
    private var player: MediaPlayer? = null

    fun playUrl(url: String, onError: (String) -> Unit) {
        runCatching {
            release()
            player = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ ->
                    onError("TTS 音频播放失败")
                    true
                }
                prepareAsync()
            }
        }.onFailure { onError("TTS 音频播放失败：${it.message}") }
    }

    fun playBase64(base64: String, mimeType: String?, onError: (String) -> Unit) {
        runCatching {
            val extension = if (mimeType?.contains("wav") == true) "wav" else "mp3"
            val file = File(context.cacheDir, "ai_reply_tts.$extension")
            file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
            playUrl(file.absolutePath, onError)
        }.onFailure { onError("TTS 音频解析失败：${it.message}") }
    }

    fun pause() = player?.pause()
    fun resume() = player?.start()

    fun release() {
        player?.release()
        player = null
    }
}
