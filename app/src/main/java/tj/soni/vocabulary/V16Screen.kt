package tj.soni.vocabulary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import tj.soni.vocabulary.data.database.*
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun V16Screen(database: AppDatabase, padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val words by database.wordDao().getAllWords()
        .collectAsState(initial = emptyList())

    var page by remember { mutableStateOf("home") }
    var selectedCategory by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf(emptyList<WordEntity>()) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var backupMessage by remember { mutableStateOf("") }

    val saveBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = createBackup(database)
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                    backupMessage = "Нусхаи эҳтиётӣ сабт шуд."
                } catch (e: Exception) {
                    backupMessage = "Хато: ${e.message}"
                }
            }
        }
    }

    val restoreBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = context.contentResolver.openInputStream(uri)!!
                        .bufferedReader().use { it.readText() }
                    restoreData(database, json)
                    backupMessage = "Маълумот барқарор карда шуд."
                } catch (e: Exception) {
                    backupMessage = "Хато: ${e.message}"
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp)
    ) {
        Text(
            "Маркази асбобҳо",
            color = Color.White,
            fontSize = 23.sp
        )
        Spacer(Modifier.height(16.dp))

        when (page) {
            "home" -> {
                FeatureButton("📝 Тест ва имтиҳон") {
                    questions = words.shuffled().take(10)
                    questionIndex = 0
                    score = 0
                    selectedAnswer = null
                    finished = false
                    page = "quiz"
                }
                FeatureButton("📚 Мавзӯъҳои омӯзиш") {
                    page = "topics"
                }
                FeatureButton("💾 Нусхаи эҳтиётӣ") {
                    page = "backup"
                }
            }

            "quiz" -> {
                Text("Тест ва имтиҳон", color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))

                if (words.size < 2) {
                    Text("Барои тест ҳадди ақал 2 калима лозим аст.", color = Color.White)
                } else if (finished || questionIndex >= questions.size) {
                    Text(
                        "Натиҷа: $score аз ${questions.size}",
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    FeatureButton("Такрори тест") {
                        questions = words.shuffled().take(10)
                        questionIndex = 0
                        score = 0
                        selectedAnswer = null
                        finished = false
                    }
                    FeatureButton("Бозгашт") { page = "home" }
                } else {
                    val question = questions[questionIndex]
                    val options = remember(question.id, words) {
                        (words.filter { it.id != question.id }
                            .shuffled().take(3) + question).shuffled()
                    }

                    Text(
                        "Савол ${questionIndex + 1} аз ${questions.size}",
                        color = Color.LightGray
                    )
                    Text(
                        question.english,
                        color = Color.White,
                        fontSize = 25.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    options.forEach { option ->
                        FeatureButton(option.tajik) {
                            if (selectedAnswer == null) {
                                selectedAnswer = option.id
                                if (option.id == question.id) score++
                            }
                        }
                    }

                    selectedAnswer?.let {
                        Text(
                            if (it == question.id) "Ҷавоби дуруст! ✓"
                            else "Ҷавоби дуруст: ${question.tajik}",
                            color = Color.White
                        )
                        FeatureButton("Саволи навбатӣ") {
                            questionIndex++
                            selectedAnswer = null
                            if (questionIndex >= questions.size) finished = true
                        }
                    }
                }
                FeatureButton("Бозгашт") { page = "home" }
            }

            "topics" -> {
                Text("Мавзӯъҳои омӯзиш", color = Color.White, fontSize = 21.sp)
                val categories = words.map { it.category }
                    .distinct().sorted()

                if (selectedCategory.isEmpty()) {
                    categories.forEach { category ->
                        FeatureButton("$category (${words.count { it.category == category }})") {
                            selectedCategory = category
                        }
                    }
                } else {
                    FeatureButton("← Ҳамаи мавзӯъҳо") {
                        selectedCategory = ""
                    }
                    LazyColumn {
                        items(words.filter { it.category == selectedCategory }) { word ->
                            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                                Text(word.english, color = Color.White, fontSize = 18.sp)
                                Text(word.tajik, color = Color.LightGray)
                                Text(word.russian, color = Color.Gray)
                            }
                            HorizontalDivider(color = Color.DarkGray)
                        }
                    }
                }
                FeatureButton("Бозгашт") {
                    selectedCategory = ""
                    page = "home"
                }
            }

            "backup" -> {
                Text("Нусхаи эҳтиётӣ", color = Color.White, fontSize = 21.sp)
                Text(
                    "Калимаҳо, дӯстдоштаҳо ва пешрафти омӯзиш.",
                    color = Color.LightGray
                )
                FeatureButton("Сохтани Backup") {
                    saveBackup.launch("soni_vocabulary_backup.json")
                }
                FeatureButton("Барқарор кардани Backup") {
                    restoreBackup.launch(arrayOf("application/json", "text/*"))
                }
                if (backupMessage.isNotEmpty()) {
                    Text(backupMessage, color = Color.White)
                }
                FeatureButton("Бозгашт") { page = "home" }
            }
        }
    }
}

@Composable
private fun FeatureButton(title: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F42C1))
    ) {
        Text(title)
    }
}

private suspend fun createBackup(db: AppDatabase): String {
    val words = db.wordDao().getAllWords().first()
    val favorites = db.favoriteWordDao().getFavorites().first()
    val progress = db.wordProgressDao().getAllProgress()

    val root = JSONObject()
    root.put("version", 1)

    root.put("words", JSONArray().apply {
        words.forEach { w ->
            put(JSONObject().apply {
                put("id", w.id)
                put("english", w.english)
                put("tajik", w.tajik)
                put("russian", w.russian)
                put("category", w.category)
                put("level", w.level)
                put("example", w.example)
            })
        }
    })

    root.put("favorites", JSONArray().apply {
        favorites.forEach {
            put(JSONObject().apply {
                put("wordId", it.wordId)
                put("addedAt", it.addedAt)
            })
        }
    })

    root.put("progress", JSONArray().apply {
        progress.forEach {
            put(JSONObject().apply {
                put("wordId", it.wordId)
                put("correctCount", it.correctCount)
                put("wrongCount", it.wrongCount)
                put("lastReviewedAt", it.lastReviewedAt)
                put("isLearned", it.isLearned)
            })
        }
    })
    return root.toString(2)
}

private suspend fun restoreData(db: AppDatabase, text: String) {
    val root = JSONObject(text)
    require(root.optInt("version") == 1) { "Нусхаи нодуруст." }

    db.withTransaction {
        val words = root.getJSONArray("words")
        val wordList = (0 until words.length()).map { i ->
            val o = words.getJSONObject(i)
            WordEntity(
                id = o.getString("id"),
                english = o.getString("english"),
                tajik = o.getString("tajik"),
                russian = o.getString("russian"),
                category = o.optString("category", "General"),
                level = o.optString("level", "Beginner"),
                example = o.optString("example", "")
            )
        }
        db.wordDao().upsertAll(wordList)

        val favorites = root.optJSONArray("favorites") ?: JSONArray()
        for (i in 0 until favorites.length()) {
            val o = favorites.getJSONObject(i)
            db.favoriteWordDao().addFavorite(
                FavoriteWordEntity(
                    wordId = o.getString("wordId"),
                    addedAt = o.optLong("addedAt")
                )
            )
        }

        val progress = root.optJSONArray("progress") ?: JSONArray()
        for (i in 0 until progress.length()) {
            val o = progress.getJSONObject(i)
            db.wordProgressDao().saveProgress(
                WordProgressEntity(
                    wordId = o.getString("wordId"),
                    correctCount = o.optInt("correctCount"),
                    wrongCount = o.optInt("wrongCount"),
                    lastReviewedAt = o.optLong("lastReviewedAt"),
                    isLearned = o.optBoolean("isLearned")
                )
            )
        }
    }
}
