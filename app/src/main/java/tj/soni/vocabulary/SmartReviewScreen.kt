package tj.soni.vocabulary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import tj.soni.vocabulary.data.database.*

@Composable
fun SmartReviewScreen(database: AppDatabase, padding: PaddingValues) {
    val scope = rememberCoroutineScope()
    val words by database.wordDao().getAllWords()
        .collectAsState(initial = emptyList())

    var progress by remember { mutableStateOf(emptyList<WordProgressEntity>()) }
    var session by remember { mutableStateOf(emptyList<WordEntity>()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var correctAnswers by remember { mutableIntStateOf(0) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(database) {
        progress = database.wordProgressDao().getAllProgress()
    }

    val progressMap = remember(progress) {
        progress.associateBy { it.wordId }
    }

    val now = System.currentTimeMillis()

    val dueWords = words.filter { word ->
        val p = progressMap[word.id]

        if (p == null || p.lastReviewedAt == 0L) {
            true
        } else {
            val days = when {
                p.wrongCount > p.correctCount -> 1
                p.correctCount <= 0 -> 0
                p.correctCount == 1 -> 1
                p.correctCount == 2 -> 3
                p.correctCount == 3 -> 7
                else -> 14
            }

            now >= p.lastReviewedAt + days * 24L * 60L * 60L * 1000L
        }
    }

    val question = session.getOrNull(index)

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Такрори ҳушманд",
            color = Color.White,
            fontSize = 24.sp
        )

        Text(
            "Калимаҳои муҳлаташон расида: ${dueWords.size}",
            color = Color.LightGray
        )

        if (!started) {
            Text(
                "Калимаҳои душвор зудтар ва калимаҳои осон баъд аз фосилаи дарозтар такрор мешаванд.",
                color = Color.White
            )

            Button(
                onClick = {
                    session = dueWords.shuffled()
                    index = 0
                    correctAnswers = 0
                    selected = null
                    started = true
                },
                enabled = dueWords.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Оғози такрор")
            }

            if (dueWords.isEmpty()) {
                Text(
                    "Офарин! Ҳоло калимаи муҳлаташрасида нест.",
                    color = Color(0xFF7EE0A3)
                )
            }
        } else if (question == null) {
            Text(
                "Анҷом ёфт! Ҷавобҳои дуруст: $correctAnswers аз ${session.size}",
                color = Color(0xFF7EE0A3),
                fontSize = 20.sp
            )

            Button(
                onClick = { started = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Бозгашт")
            }
        } else {
            Text(
                "Калимаи ${index + 1} аз ${session.size}",
                color = Color.LightGray
            )

            Text(
                question.english,
                color = Color.White,
                fontSize = 28.sp
            )

            if (question.example.isNotBlank()) {
                Text(question.example, color = Color.LightGray)
            }

            val options = remember(question.id, words) {
                (
                    words.filter {
                        it.id != question.id && it.tajik.isNotBlank()
                    }.shuffled().take(3) + question
                ).distinctBy { it.id }.shuffled()
            }

            options.forEach { option ->
                val isCorrect = option.id == question.id

                Button(
                    onClick = {
                        if (selected == null) {
                            selected = option.id

                            if (isCorrect) {
                                correctAnswers++
                            }

                            scope.launch {
                                val dao = database.wordProgressDao()

                                val old = dao.getProgress(question.id)
                                    ?: WordProgressEntity(wordId = question.id)

                                val updated = old.copy(
                                    correctCount = old.correctCount +
                                        (if (isCorrect) 1 else 0),
                                    wrongCount = old.wrongCount +
                                        (if (isCorrect) 0 else 1),
                                    lastReviewedAt = System.currentTimeMillis(),
                                    isLearned = old.isLearned ||
                                        (isCorrect && old.correctCount + 1 >= 3)
                                )

                                dao.saveProgress(updated)

                                progress = progress
                                    .filterNot { it.wordId == question.id } +
                                    updated
                            }
                        }
                    },
                    enabled = selected == null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            selected == null ->
                                MaterialTheme.colorScheme.primary

                            isCorrect -> Color(0xFF238B57)

                            selected == option.id ->
                                Color(0xFFB3261E)

                            else ->
                                MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(option.tajik)
                }
            }

            if (selected != null) {
                Text(
                    if (selected == question.id)
                        "Ҷавоби дуруст!"
                    else
                        "Ҷавоби дуруст: ${question.tajik}",
                    color = Color.White
                )

                Button(
                    onClick = {
                        index++
                        selected = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (index + 1 >= session.size)
                            "Анҷом"
                        else
                            "Калимаи навбатӣ"
                    )
                }
            }
        }
    }
}
