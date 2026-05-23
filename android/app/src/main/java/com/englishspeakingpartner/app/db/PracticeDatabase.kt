package com.englishspeakingpartner.app.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "practice_history")
data class PracticeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

@Dao
interface PracticeHistoryDao {
    @Query("SELECT * FROM practice_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PracticeHistoryEntity>>

    @Insert
    suspend fun insert(entity: PracticeHistoryEntity)

    @Query("DELETE FROM practice_history")
    suspend fun clear()
}

@Database(entities = [PracticeHistoryEntity::class], version = 1, exportSchema = false)
abstract class PracticeDatabase : RoomDatabase() {
    abstract fun historyDao(): PracticeHistoryDao
}
