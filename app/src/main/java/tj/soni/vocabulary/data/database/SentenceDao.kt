package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {

    @Upsert
    suspend fun upsert(sentence: SentenceEntity)

    @Upsert
    suspend fun upsertAll(sentences: List<SentenceEntity>)

    @Query("SELECT * FROM sentences ORDER BY id ASC")
    fun getAllSentences(): Flow<List<SentenceEntity>>

    @Query("""
        SELECT * FROM sentences
        WHERE english LIKE '%' || :query || '%'
        OR tajik LIKE '%' || :query || '%'
        OR russian LIKE '%' || :query || '%'
        OR chinese LIKE '%' || :query || '%'
        OR chinesePinyin LIKE '%' || :query || '%'
        OR chinesePronunciation LIKE '%' || :query || '%'
        LIMIT 100
    """)
    fun searchSentences(query: String): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences WHERE level = :level")
    fun getSentencesByLevel(level: String): Flow<List<SentenceEntity>>

    @Query("SELECT COUNT(*) FROM sentences")
    fun getSentenceCount(): Flow<Int>

    @Query("DELETE FROM sentences")
    suspend fun deleteAll()
}
