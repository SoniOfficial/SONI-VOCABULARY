package tj.soni.vocabulary.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_schedules")
data class StudyScheduleEntity(
    @PrimaryKey val id: Int,
    val startTime: String = "20:00",
    val questionCount: Int = 20,
    val enabled: Boolean = false,
    val active: Boolean = false,
    val sessionDate: String = "",
    val questionIds: String = "",
    val currentIndex: Int = 0,
    val correctCount: Int = 0
)
