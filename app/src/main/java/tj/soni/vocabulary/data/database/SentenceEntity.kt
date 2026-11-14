package tj.soni.vocabulary.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sentences",
    indices = [
        Index(value = ["level"]),
        Index(value = ["category"]),
        Index(value = ["chinese"])
    ]
)
data class SentenceEntity(
    @PrimaryKey
    val id: String,
    val english: String,
    val tajik: String,
    val russian: String,
    @ColumnInfo(defaultValue = "")
    val chinese: String = "",
    @ColumnInfo(defaultValue = "")
    val chinesePinyin: String = "",
    @ColumnInfo(defaultValue = "")
    val chinesePronunciation: String = "",
    val level: String = "Beginner",
    val category: String = "General"
)
