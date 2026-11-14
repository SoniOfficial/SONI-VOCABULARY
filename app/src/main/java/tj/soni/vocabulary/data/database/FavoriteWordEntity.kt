package tj.soni.vocabulary.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_words")
data class FavoriteWordEntity(
    @PrimaryKey
    val wordId: String,
    val addedAt: Long = System.currentTimeMillis()
)
