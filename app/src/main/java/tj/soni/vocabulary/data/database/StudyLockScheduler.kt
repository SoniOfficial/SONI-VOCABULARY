package tj.soni.vocabulary.data.database

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object StudyLockScheduler {

    suspend fun checkAndStart(database: AppDatabase) {
        val dao = database.studyLockDao()
        val settings = dao.get() ?: return

        if (!settings.enabled || settings.active) return

        val today = LocalDate.now().toString()
        if (settings.sessionDate == today) return

        val scheduledTime = try {
            LocalTime.parse(
                settings.startTime,
                DateTimeFormatter.ofPattern("HH:mm")
            )
        } catch (_: Exception) {
            return
        }

        if (LocalTime.now().isBefore(scheduledTime)) return

        val selectedWords = dao.getRandomWords(
            settings.questionCount.coerceIn(1, 500)
        )

        if (selectedWords.isEmpty()) return

        dao.save(
            settings.copy(
                active = true,
                sessionDate = today,
                questionIds = selectedWords.joinToString("\n") { it.id },
                currentIndex = 0,
                correctCount = 0
            )
        )
    }
}
