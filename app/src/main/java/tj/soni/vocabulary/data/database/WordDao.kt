package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {

    @Upsert
    suspend fun upsert(word: WordEntity)

    @Upsert
    suspend fun upsertAll(words: List<WordEntity>)

    @Query("SELECT * FROM words ORDER BY english ASC")
    fun getAllWords(): Flow<List<WordEntity>>

    @Query("""
        SELECT * FROM words
        WHERE english LIKE '%' || :query || '%'
        OR tajik LIKE '%' || :query || '%'
        OR russian LIKE '%' || :query || '%'
        OR chinese LIKE '%' || :query || '%'
        OR chinesePinyin LIKE '%' || :query || '%'
        OR chinesePronunciation LIKE '%' || :query || '%'
        ORDER BY english ASC
        LIMIT 100
    """)
    fun searchWords(query: String): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE level = :level ORDER BY english ASC")
    fun getWordsByLevel(level: String): Flow<List<WordEntity>>

    @Query("SELECT COUNT(*) FROM words")
    fun getWordCount(): Flow<Int>

    @Query("DELETE FROM words")
    suspend fun deleteAll()
}
