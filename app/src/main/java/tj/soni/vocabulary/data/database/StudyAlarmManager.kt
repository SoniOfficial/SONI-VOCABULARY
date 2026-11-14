package tj.soni.vocabulary.data.database

import tj.soni.vocabulary.StudyAlarmReceiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tj.soni.vocabulary.MainActivity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object StudyAlarmManager {

    private const val CHANNEL_ID = "soni_study_channel"

    fun schedule(context: Context, schedule: StudyScheduleEntity) {
        if (!schedule.enabled) {
            cancel(context, schedule.id)
            return
        }

        val time = try {
            LocalTime.parse(
                schedule.startTime,
                DateTimeFormatter.ofPattern("HH:mm")
            )
        } catch (_: Exception) {
            return
        }

        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now(), time)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        setAlarm(context, schedule.id, target)
    }

    private fun setAlarm(
        context: Context,
        id: Int,
        target: LocalDateTime
    ) {
        val intent = Intent(context, StudyAlarmReceiver::class.java)
            .putExtra("schedule_id", id)

        val pending = PendingIntent.getBroadcast(
            context,
            100 + id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val millis = target.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        val showIntent = PendingIntent.getActivity(
            context,
            400 + id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val alarmInfo = AlarmManager.AlarmClockInfo(millis, showIntent)
            alarmManager.setAlarmClock(alarmInfo, pending)
            Log.i("LEXIO_ALARM", "AlarmClock set: id=$id time=$target")
        } catch (e: Exception) {
            Log.e("LEXIO_ALARM", "AlarmClock failed for id=$id", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    millis,
                    pending
                )
                Log.i("LEXIO_ALARM", "Exact fallback set: id=$id")
            } catch (error: Exception) {
                Log.e("LEXIO_ALARM", "Fallback failed for id=$id", error)
            }
        }
    }

    fun cancel(context: Context, id: Int) {
        val intent = Intent(context, StudyAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            100 + id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pending != null) {
            val manager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.cancel(pending)
            pending.cancel()
        }
    }

    suspend fun scheduleAll(context: Context) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val schedules = database.studyScheduleDao().getAll()

        for (schedule in schedules) {
            if (!schedule.enabled) {
                cancel(context, schedule.id)
                continue
            }

            val time = try {
                LocalTime.parse(schedule.startTime)
            } catch (_: Exception) {
                continue
            }

            val now = LocalDateTime.now()
            val today = LocalDate.now().toString()
            val target = LocalDateTime.of(LocalDate.now(), time)

            if (schedule.sessionDate != today && !target.isAfter(now)) {
                startSession(context, schedule.id)
            } else {
                schedule(context, schedule)
            }
        }
    }

    suspend fun startSession(context: Context, id: Int) =
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(context)
            val dao = database.studyScheduleDao()
            val schedule = dao.getById(id) ?: return@withContext

            if (!schedule.enabled || schedule.active) {
                return@withContext
            }

            val today = LocalDate.now().toString()
            if (schedule.sessionDate == today) {
                return@withContext
            }

            // Як машғулият дар як вақт фаъол мешавад.
            if (dao.getAll().any { it.active }) {
                return@withContext
            }

            val words = dao.getRandomWords(
                schedule.questionCount.coerceIn(1, 500)
            )

            if (words.isEmpty()) return@withContext

            dao.save(
                schedule.copy(
                    active = true,
                    sessionDate = today,
                    questionIds = words.joinToString("\n") { it.id },
                    currentIndex = 0,
                    correctCount = 0
                )
            )

            showNotification(context, id)
        }

    fun showNotification(context: Context, id: Int) {
        createChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_study", true)
            putExtra("schedule_id", id)
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            200 + id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("LEXIO")
            .setContentText("Вақти омӯзиш расид. Барои оғоз пахш кунед.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(300 + id, notification)
        } catch (_: SecurityException) {
            // Агар иҷозати огоҳинома дода нашуда бошад.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Огоҳиномаҳои омӯзиш",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Вақти машғулиятҳои LEXIO"
            }

            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }
}
