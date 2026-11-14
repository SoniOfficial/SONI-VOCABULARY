package tj.soni.vocabulary

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalTime
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.StudyAlarmManager
import tj.soni.vocabulary.data.database.StudyScheduleEntity
import tj.soni.vocabulary.data.database.WordEntity
import tj.soni.vocabulary.data.database.WordProgressEntity

@Composable
fun StudyScreen(
    database: AppDatabase,
    padding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(database) { database.studyScheduleDao() }
    val schedules by dao.observeAll().collectAsState(initial = emptyList())

    val activeSchedule = schedules.firstOrNull { it.active }

    var words by remember { mutableStateOf(emptyList<WordEntity>()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(activeSchedule?.id, activeSchedule?.questionIds) {
        selectedAnswer = null
        val active = activeSchedule
        if (active == null) {
            words = emptyList()
        } else {
            loading = true
            val ids = active.questionIds.split("\n").filter { it.isNotBlank() }
            val found = if (ids.isEmpty()) emptyMap()
            else dao.getWordsByIds(ids).associateBy { it.id }
            words = ids.mapNotNull { found[it] }
            loading = false
        }
    }

    val active = activeSchedule != null
    val currentIndex = activeSchedule?.currentIndex ?: 0
    val question = words.getOrNull(currentIndex)

    BackHandler(enabled = active) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (active) PaddingValues(0.dp) else padding)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (active) "🔒 Машғулияти омӯзиш" else "Маркази омӯзиш",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        if (activeSchedule != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Ҷадвали ${activeSchedule.id} фаъол аст",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    LinearProgressIndicator(
                        progress = {
                            if (words.isEmpty()) 0f
                            else currentIndex.toFloat() / words.size
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Савол: ${minOf(currentIndex + 1, words.size)} аз ${words.size}")

                    if (loading) {
                        CircularProgressIndicator()
                    } else if (question == null && words.isNotEmpty()) {
                        Text("Омӯзиш анҷом ёфт!")
                    } else if (question == null) {
                        Text("Саволҳо бор нашуданд. Луғатро санҷед.")
                    } else {
                        Text(
                            question.english,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (question.example.isNotBlank()) {
                            Text(question.example)
                        }

                        val options = remember(question.id, words) {
                            (
                                words.filter {
                                    it.id != question.id && it.tajik.isNotBlank()
                                }.map { it.tajik }.distinct().shuffled().take(3) +
                                    question.tajik
                                ).distinct().shuffled()
                        }

                        options.forEach { answer ->
                            val correct = answer == question.tajik
                            val chosen = selectedAnswer == answer

                            Button(
                                onClick = {
                                    if (selectedAnswer == null) {
                                        selectedAnswer = answer

                                        scope.launch {
                                            val progressDao = database.wordProgressDao()
                                            val old = progressDao.getProgress(question.id)
                                                ?: WordProgressEntity(wordId = question.id)

                                            progressDao.saveProgress(
                                                old.copy(
                                                    correctCount = old.correctCount +
                                                        if (correct) 1 else 0,
                                                    wrongCount = old.wrongCount +
                                                        if (correct) 0 else 1,
                                                    lastReviewedAt = System.currentTimeMillis(),
                                                    isLearned = old.isLearned ||
                                                        (correct && old.correctCount + 1 >= 3)
                                                )
                                            )
                                        }
                                    }
                                },
                                enabled = selectedAnswer == null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        selectedAnswer == null ->
                                            MaterialTheme.colorScheme.primary
                                        correct -> Color(0xFF238B57)
                                        chosen -> Color(0xFFB3261E)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Text(answer)
                            }
                        }

                        if (selectedAnswer != null) {
                            Text(
                                if (selectedAnswer == question.tajik)
                                    "Офарин! Ҷавоби дуруст аст. ✓"
                                else
                                    "Ҷавоби дуруст: ${question.tajik}",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAnswer == question.tajik)
                                    Color(0xFF4CAF80)
                                else
                                    Color(0xFFFF7777)
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        val latest = dao.getById(activeSchedule.id)
                                            ?: return@launch
                                        val next = latest.currentIndex + 1

                                        if (next >= words.size) {
                                            dao.save(
                                                latest.copy(
                                                    active = false,
                                                    currentIndex = words.size
                                                )
                                            )
                                        } else {
                                            dao.save(latest.copy(currentIndex = next))
                                            selectedAnswer = null
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (currentIndex == words.lastIndex)
                                        "Анҷом додан"
                                    else "Саволи навбатӣ"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "Се вақти мустақил барои омӯзиши ҳаррӯза",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            schedules.sortedBy { it.id }.forEach { schedule ->
                StudyScheduleCard(
                    schedule = schedule,
                    onSave = { updated ->
                        scope.launch {
                            dao.save(updated)
                            StudyAlarmManager.schedule(context, updated)
                        }
                    },
                    onDisable = {
                        scope.launch {
                            val latest = dao.getById(schedule.id) ?: return@launch
                            val updated = latest.copy(enabled = false, active = false)
                            dao.save(updated)
                            StudyAlarmManager.cancel(context, schedule.id)
                        }
                    }
                )
            }

            Text(
                "Дар вақти интихобшуда, агар барнома баста бошад, огоҳинома меояд. Барои кушодани машғулият онро пахш кунед.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StudyScheduleCard(
    schedule: StudyScheduleEntity,
    onSave: (StudyScheduleEntity) -> Unit,
    onDisable: () -> Unit
) {
    val context = LocalContext.current

    var timeText by remember(schedule.id, schedule.startTime) {
        mutableStateOf(schedule.startTime)
    }
    var countText by remember(schedule.id, schedule.questionCount) {
        mutableStateOf(schedule.questionCount.toString())
    }
    var saving by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Ҷадвали ${schedule.id}",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Text("Вақти саршавӣ")

            OutlinedButton(
                onClick = {
                    val initial = try {
                        LocalTime.parse(timeText)
                    } catch (_: Exception) {
                        LocalTime.of(20, 0)
                    }

                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            timeText = String.format(
                                java.util.Locale.US,
                                "%02d:%02d",
                                hour,
                                minute
                            )
                        },
                        initial.hour,
                        initial.minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🕒  $timeText", fontSize = 21.sp)
            }

            Text("Шумораи саволҳо")

            OutlinedTextField(
                value = countText,
                onValueChange = {
                    countText = it.filter { char -> char.isDigit() }.take(3)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Аз 1 то 500 савол") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(10, 20, 50, 100).forEach { number ->
                    OutlinedButton(
                        onClick = { countText = number.toString() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(number.toString())
                    }
                }
            }

            Button(
                onClick = {
                    val count = countText.toIntOrNull()
                    if (count != null && count in 1..500) {
                        saving = true
                        onSave(
                            schedule.copy(
                                startTime = timeText,
                                questionCount = count,
                                enabled = true,
                                active = false,
                                sessionDate = ""
                            )
                        )
                        saving = false
                    }
                },
                enabled = !saving &&
                    (countText.toIntOrNull() ?: 0) in 1..500,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (schedule.enabled) "Навсозии нақша" else "Фаъол кардани нақша")
            }

            if (schedule.enabled) {
                Text(
                    "Фаъол аст: ҳар рӯз соати ${schedule.startTime}, ${schedule.questionCount} савол.",
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = onDisable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Хомӯш кардани ин нақша")
                }
            }
        }
    }
}
