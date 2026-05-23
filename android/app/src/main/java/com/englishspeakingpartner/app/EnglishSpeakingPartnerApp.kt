package com.englishspeakingpartner.app

import android.app.Application
import androidx.room.Room
import com.englishspeakingpartner.app.db.PracticeDatabase
import com.englishspeakingpartner.app.network.ModelRemoteDataSource
import com.englishspeakingpartner.app.repo.HistoryRepository
import com.englishspeakingpartner.app.repo.PracticeRepository
import com.englishspeakingpartner.app.repo.SettingsRepository

class EnglishSpeakingPartnerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(
            this,
            PracticeDatabase::class.java,
            "english_speaking_partner.db"
        ).build()
        val settingsRepository = SettingsRepository(this)
        val historyRepository = HistoryRepository(db.historyDao())
        container = AppContainer(
            settingsRepository = settingsRepository,
            historyRepository = historyRepository,
            practiceRepository = PracticeRepository(ModelRemoteDataSource(), historyRepository)
        )
    }
}

data class AppContainer(
    val settingsRepository: SettingsRepository,
    val historyRepository: HistoryRepository,
    val practiceRepository: PracticeRepository
)
