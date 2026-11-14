package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLockDao {

    @Upsert
    suspend fun save(lock: StudyLockEntity)

    @Query("SELECT * FROM study_lock WHERE id = 1")
    fun observe(): Flow<StudyLockEntity?>

    @Query("SELECT * FROM study_lock WHERE id = 1")
    suspend fun get(): StudyLockEntity?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWordsByIds(ids: List<String>): List<WordEntity>
}
