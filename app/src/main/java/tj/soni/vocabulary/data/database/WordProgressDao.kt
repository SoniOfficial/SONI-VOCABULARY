package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordProgressDao {

    @Upsert
    suspend fun saveProgress(progress: WordProgressEntity)

    @Query("SELECT * FROM word_progress WHERE wordId = :wordId LIMIT 1")
    suspend fun getProgress(wordId: String): WordProgressEntity?

    @Query("SELECT * FROM word_progress")
    suspend fun getAllProgress(): List<WordProgressEntity>

    @Query("SELECT COUNT(*) FROM word_progress WHERE isLearned = 1")
    fun getLearnedWordCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(correctCount), 0) FROM word_progress")
    fun getTotalCorrectAnswers(): Flow<Int>

    @Query("SELECT COALESCE(SUM(wrongCount), 0) FROM word_progress")
    fun getTotalWrongAnswers(): Flow<Int>
}
