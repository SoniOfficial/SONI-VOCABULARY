package tj.soni.vocabulary.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_progress")
data class WordProgressEntity(
    @PrimaryKey
    val wordId: String,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lastReviewedAt: Long = 0L,
    val isLearned: Boolean = false
)
