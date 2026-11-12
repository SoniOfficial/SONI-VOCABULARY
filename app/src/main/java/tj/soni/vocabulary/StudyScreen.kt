package tj.soni.vocabulary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.DailySessionEntity
import tj.soni.vocabulary.data.database.WordEntity
import tj.soni.vocabulary.data.database.WordProgressEntity

@Composable
fun StudyScreen(
    database: AppDatabase,
    padding: PaddingValues
) {
    val today = LocalDate.now().toString()
    val scope = rememberCoroutineScope()

    val wordFlow = remember(database) {
        database.wordDao().getAllWords()
    }

    val sessionFlow = remember(database, today) {
        database.dailySessionDao().getSessionsForDate(today)
    }

    val words by wordFlow.collectAsState(initial = emptyList())
    val sessions by sessionFlow.collectAsState(initial = emptyList())

    var selectedSession by remember { mutableIntStateOf(1) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }

    val questions = remember(words, selectedSession) {
        words.shuffled().take(5)
    }

    val question = questions.getOrNull(questionIndex)

    val options = remember(question, words) {
        if (question == null) {
            emptyList()
        } else {
            (
                words.filter {
                    it.id != question.id && it.tajik.isNotBlank()
                }.map { it.tajik }.distinct().shuffled().take(3) +
                    question.tajik
                ).shuffled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Маркази омӯзиш",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "5 машғулият • 5 савол дар ҳар машғулият",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Пешрафти имрӯз",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${sessions.count { it.completed }} / 5 машғулият иҷро шуд",
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        sessions.count { it.completed } / 5f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            "Машғулиятҳоро интихоб кун",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { number ->
                val done = sessions.any {
                    it.sessionNumber == number && it.completed
                }

                FilterChip(
                    selected = selectedSession == number,
                    onClick = {
                        selectedSession = number
                        questionIndex = 0
                        score = 0
                        selectedAnswer = null
                    },
                    label = {
                        Text(
                            if (done) "✓ $number" else "$number"
                        )
                    }
                )
            }
        }

        if (words.isEmpty()) {
            Text(
                "Ҳоло калима нест. Аввал луғатро пур кун.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (questions.isEmpty()) {
            Text("Саволҳо дастрас нестанд.")
        } else if (questionIndex >= questions.size) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", fontSize = 42.sp)
                    Text(
                        "Машғулият анҷом ёфт!",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Натиҷа: $score аз ${questions.size}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 19.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedSession = (selectedSession % 5) + 1
                            questionIndex = 0
                            score = 0
                            selectedAnswer = null
                        }
                    ) {
                        Text("Машғулияти дигар")
                    }
                }
            }
        } else if (question != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Машғулияти $selectedSession",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Саволи ${questionIndex + 1} аз ${questions.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = {
                            (questionIndex + 1).toFloat() /
                                questions.size
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    Text(
                        "Тарҷумаи ин калима чист?",
                        fontSize = 17.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        question.english,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (question.example.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            question.example,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    options.forEach { answer ->
                        val isSelected = selectedAnswer == answer
                        val isCorrect = answer == question.tajik

                        val buttonColor = when {
                            selectedAnswer == null ->
                                MaterialTheme.colorScheme.primary
                            isSelected && isCorrect ->
                                Color(0xFF238B57)
                            isSelected && !isCorrect ->
                                Color(0xFFB3261E)
                            isCorrect ->
                                Color(0xFF238B57)
                            else ->
                                MaterialTheme.colorScheme.surfaceVariant
                        }

                        Button(
                            onClick = {
                                if (selectedAnswer == null) {
                                    selectedAnswer = answer

                                    val correct = answer == question.tajik

                                    if (correct) score++

                                    scope.launch {
                                        val dao = database.wordProgressDao()
                                        val old = dao.getProgress(question.id)
                                            ?: WordProgressEntity(
                                                wordId = question.id
                                            )

                                        dao.saveProgress(
                                            old.copy(
                                                correctCount = old.correctCount +
                                                    if (correct) 1 else 0,
                                                wrongCount = old.wrongCount +
                                                    if (correct) 0 else 1,
                                                lastReviewedAt =
                                                    System.currentTimeMillis(),
                                                isLearned =
                                                    old.isLearned ||
                                                        (correct &&
                                                            old.correctCount + 1 >= 3)
                                            )
                                        )
                                    }
                                }
                            },
                            enabled = selectedAnswer == null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonColor,
                                disabledContainerColor = buttonColor,
                                contentColor = Color.White,
                                disabledContentColor = Color.White
                            )
                        ) {
                            Text(answer)
                        }
                    }

                    if (selectedAnswer != null) {
                        Spacer(Modifier.height(10.dp))

                        Text(
                            if (selectedAnswer == question.tajik)
                                "Офарин! Ҷавоби дуруст аст. ✓"
                            else
                                "Ҷавоби дуруст: ${question.tajik}",
                            color = if (selectedAnswer == question.tajik)
                                Color(0xFF4CAF80)
                            else
                                Color(0xFFFF7777),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (questionIndex < questions.lastIndex) {
                                    questionIndex++
                                    selectedAnswer = null
                                } else {
                                    scope.launch {
                                        database.dailySessionDao()
                                            .saveSession(
                                                DailySessionEntity(
                                                    sessionDate = today,
                                                    sessionNumber = selectedSession,
                                                    completed = true,
                                                    completedAt =
                                                        System.currentTimeMillis()
                                                )
                                            )
                                    }
                                    questionIndex = questions.size
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (questionIndex == questions.lastIndex)
                                    "Анҷом додан"
                                else
                                    "Саволи навбатӣ"
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Ҷавобҳои дуруст се маротиба такрор шаванд, калима ҳамчун омӯхташуда қайд мешавад.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}
