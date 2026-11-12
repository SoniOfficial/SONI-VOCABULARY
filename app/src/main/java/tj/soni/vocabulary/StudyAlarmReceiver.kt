package tj.soni.vocabulary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tj.soni.vocabulary.data.database.StudyAlarmManager

class StudyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("schedule_id", -1)
        if (id !in 1..3) return

        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                StudyAlarmManager.startSession(appContext, id)

                val database =
                    tj.soni.vocabulary.data.database.AppDatabase
                        .getDatabase(appContext)

                val schedule =
                    database.studyScheduleDao().getById(id)

                if (schedule != null) {
                    StudyAlarmManager.schedule(appContext, schedule)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
