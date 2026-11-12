package tj.soni.vocabulary.data.database

import androidx.room.Entity

@Entity(
    tableName = "daily_sessions",
    primaryKeys = ["sessionDate", "sessionNumber"]
)
data class DailySessionEntity(
    val sessionDate: String,
    val sessionNumber: Int,
    val completed: Boolean = false,
    val completedAt: Long = 0L
)
