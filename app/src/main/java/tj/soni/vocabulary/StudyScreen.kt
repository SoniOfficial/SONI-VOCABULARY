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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.StudyLockEntity
import tj.soni.vocabulary.data.database.WordEntity
import tj.soni.vocabulary.data.database.WordProgressEntity

@Composable
fun StudyScreen(
    database: AppDatabase,
    padding: PaddingValues
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember(database) { database.studyLockDao() }

    val lock by dao.observe().collectAsState(initial = null)

    var words by remember { mutableStateOf(emptyList<WordEntity>()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var timeText by remember(lock?.startTime) {
        mutableStateOf(lock?.startTime ?: "20:00")
    }
    var countText by remember(lock?.questionCount) {
        mutableStateOf((lock?.questionCount ?: 20).toString())
    }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(lock?.questionIds, lock?.active) {
        selectedAnswer = null
        words = if (lock?.active == true) {
            val ids = lock!!.questionIds
                .split("\n")
                .filter { it.isNotBlank() }

            if (ids.isEmpty()) emptyList()
            else {
                val found = dao.getWordsByIds(ids).associateBy { it.id }
                ids.mapNotNull { found[it] }
            }
        } else {
            emptyList()
        }
    }

    val activeLock = lock?.active == true
    val currentIndex = lock?.currentIndex ?: 0
    val question = words.getOrNull(currentIndex)

    // Ҳангоми қулф тугмаи Back кор намекунад.
    BackHandler(enabled = activeLock) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (activeLock) PaddingValues(0.dp) else padding)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (activeLock) "Қулфи омӯзиш фаъол аст" else "Маркази омӯзиш",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (activeLock) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🔒 То анҷоми ҳамаи саволҳо аз омӯзиш баромада намешавад.",
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

                    if (question == null && words.isNotEmpty()) {
                        Text("Омӯзиш анҷом ёфт!")
                    } else if (question == null) {
                        Text("Саволҳо бор нашуданд. Барномаро аз нав кушоед.")
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
                                        val latest = dao.get() ?: return@launch
                                        val next = latest.currentIndex + 1

                                        if (next >= words.size) {
                                            dao.save(
                                                latest.copy(
                                                    active = false,
                                                    currentIndex = words.size
                                                )
                                            )
                                        } else {
                                            dao.save(
                                                latest.copy(currentIndex = next)
                                            )
                                            selectedAnswer = null
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (currentIndex == words.lastIndex)
                                        "Анҷом додан ва кушодани қулф"
                                    else "Саволи навбатӣ"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Танзими қулфи омӯзиш",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text("Вақти саршавии ҳаррӯза")

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
                        Text(
                            "🕒  $timeText",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                                scope.launch {
                                    val old = dao.get() ?: StudyLockEntity()
                                    dao.save(
                                        old.copy(
                                            startTime = timeText,
                                            questionCount = count,
                                            enabled = true,
                                            active = false,
                                            sessionDate = ""
                                        )
                                    )
                                    saving = false
                                }
                            }
                        },
                        enabled = !saving &&
                            (countText.toIntOrNull() ?: 0) in 1..500,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (saving) "Сабт шуда истодааст..." else "Фаъол кардани қулф")
                    }

                    if (lock?.enabled == true) {
                        Text(
                            "Қулф фаъол аст. Ҳар рӯз соати ${lock!!.startTime} бо ${lock!!.questionCount} савол оғоз мешавад.",
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val latest = dao.get() ?: return@launch
                                    dao.save(
                                        latest.copy(
                                            enabled = false,
                                            active = false
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Хомӯш кардани нақша")
                        }
                    }

                    if (lock?.active == false &&
                        lock?.sessionDate == LocalDate.now().toString()
                    ) {
                        Text(
                            "🎉 Саволҳои имрӯз ҷавоб дода шуданд!",
                            color = Color(0xFF4CAF80),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Қулф дар вақти интихобшуда ҳангоми фаъол будани барнома ё боз кардани он оғоз мешавад.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
