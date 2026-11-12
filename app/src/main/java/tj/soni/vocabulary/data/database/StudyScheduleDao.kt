package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyScheduleDao {

    @Upsert
    suspend fun save(schedule: StudyScheduleEntity)

    @Upsert
    suspend fun saveAll(schedules: List<StudyScheduleEntity>)

    @Query("SELECT * FROM study_schedules ORDER BY id")
    fun observeAll(): Flow<List<StudyScheduleEntity>>

    @Query("SELECT * FROM study_schedules ORDER BY id")
    suspend fun getAll(): List<StudyScheduleEntity>

    @Query("SELECT * FROM study_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): StudyScheduleEntity?

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWordsByIds(ids: List<String>): List<WordEntity>
}
