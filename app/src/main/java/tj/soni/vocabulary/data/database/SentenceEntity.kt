package tj.soni.vocabulary.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sentences",
    indices = [
        Index(value = ["level"]),
        Index(value = ["category"])
    ]
)
data class SentenceEntity(
    @PrimaryKey
    val id: String,
    val english: String,
    val tajik: String,
    val russian: String,
    val level: String = "Beginner",
    val category: String = "General"
)
