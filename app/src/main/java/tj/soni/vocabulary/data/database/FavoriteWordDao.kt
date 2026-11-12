package tj.soni.vocabulary.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteWordDao {
    @Upsert
    suspend fun addFavorite(item: FavoriteWordEntity)

    @Query("DELETE FROM favorite_words WHERE wordId = :wordId")
    suspend fun removeFavorite(wordId: String)

    @Query("SELECT * FROM favorite_words ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteWordEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_words WHERE wordId = :wordId)")
    fun isFavorite(wordId: String): Flow<Boolean>
}
