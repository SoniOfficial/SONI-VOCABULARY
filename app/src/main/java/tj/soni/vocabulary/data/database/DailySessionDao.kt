package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySessionDao {

    @Upsert
    suspend fun saveSession(session: DailySessionEntity)

    @Query("""
        SELECT * FROM daily_sessions
        WHERE sessionDate = :date
        ORDER BY sessionNumber ASC
    """)
    fun getSessionsForDate(date: String): Flow<List<DailySessionEntity>>

    @Query("""
        SELECT COUNT(*) FROM daily_sessions
        WHERE sessionDate = :date AND completed = 1
    """)
    fun getCompletedCount(date: String): Flow<Int>
}
