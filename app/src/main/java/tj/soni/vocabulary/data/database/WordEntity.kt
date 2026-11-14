package tj.soni.vocabulary.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["english"]),
        Index(value = ["tajik"]),
        Index(value = ["russian"]),
        Index(value = ["chinese"]),
        Index(value = ["category"]),
        Index(value = ["level"])
    ]
)
data class WordEntity(
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
    val category: String = "General",
    val level: String = "Beginner",
    val example: String = ""
)
