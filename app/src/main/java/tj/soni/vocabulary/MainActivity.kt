package tj.soni.vocabulary
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.StudyAlarmManager
import tj.soni.vocabulary.data.database.StudyScheduleEntity
import tj.soni.vocabulary.data.database.WordEntity
import tj.soni.vocabulary.data.database.FavoriteWordEntity
import android.speech.tts.TextToSpeech
import java.util.Locale
import tj.soni.vocabulary.data.importer.ContentImporter

private val Background = Color(0xFF100B20)
private val SurfaceDark = Color(0xFF1D1533)
private val Purple = Color(0xFF9B72FF)
private val TextWhite = Color(0xFFF8F5FF)
private val Muted = Color(0xFFB8ADC9)

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }

        setContent {
            LaunchedEffect(Unit) {
                delay(1800)
                keepSplash = false
            }
            VocabularyApp()
        }
    }
}

@Composable
fun VocabularyApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val scheduleFlow = remember(database) {
        database.studyScheduleDao().observeAll()
    }
    val schedules by scheduleFlow.collectAsState(initial = emptyList())

    val completedDatesFlow = remember(database) {
        database.dailySessionDao().observeCompletedDates()
    }
    val completedDates by completedDatesFlow.collectAsState(initial = emptyList())

    val favoriteFlow = remember(database) {
        database.favoriteWordDao().getFavorites()
    }
    val favoriteItems by favoriteFlow.collectAsState(initial = emptyList())
    val favoriteIds = remember(favoriteItems) {
        favoriteItems.map { it.wordId }.toSet()
    }

    val learnedCount by database.wordProgressDao()
        .getLearnedWordCount().collectAsState(initial = 0)
    val correctAnswers by database.wordProgressDao()
        .getTotalCorrectAnswers().collectAsState(initial = 0)
    val wrongAnswers by database.wordProgressDao()
        .getTotalWrongAnswers().collectAsState(initial = 0)

    val hasActiveStudy = schedules.any { it.active }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(database) {
        val dao = database.studyScheduleDao()
        val existingIds = dao.getAll().map { it.id }.toSet()

        val defaults = listOf(
            StudyScheduleEntity(id = 1, startTime = "09:00"),
            StudyScheduleEntity(id = 2, startTime = "14:00"),
            StudyScheduleEntity(id = 3, startTime = "18:00")
        )

        val missing = defaults.filter { it.id !in existingIds }

        if (missing.isNotEmpty()) {
            dao.saveAll(missing)
        }

        StudyAlarmManager.scheduleAll(context)
    }

    LaunchedEffect(hasActiveStudy) {
        if (hasActiveStudy) {
            selectedTab = 2
        }
    }

    val importer = remember { ContentImporter(context, database) }
    var language by remember { mutableStateOf("Тоҷикӣ") }
    var searchQuery by remember { mutableStateOf("") }
    var words by remember { mutableStateOf(emptyList<WordEntity>()) }

    LaunchedEffect(database) {
        val preferences = context.getSharedPreferences(
            "soni_content_preferences",
            android.content.Context.MODE_PRIVATE
        )

        val savedVersion = preferences.getInt("content_version", 0)
        val currentVersion = 1

        if (savedVersion < currentVersion) {
            val wordResult = importer.importWordsFromAssets(
                "content/words/english.json"
            )
            val sentenceResult = importer.importSentencesFromAssets(
                "content/sentences/daily.json"
            )

            if (wordResult.failed == 0 && sentenceResult.failed == 0) {
                preferences.edit()
                    .putInt("content_version", currentVersion)
                    .apply()
            }
        }
    }

    LaunchedEffect(database, searchQuery) {
        database.wordDao()
            .searchWords(searchQuery.trim())
            .collect { result ->
                words = result
            }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            background = Background,
            surface = SurfaceDark,
            onBackground = TextWhite,
            onSurface = TextWhite
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                if (!hasActiveStudy) {
                    NavigationBar(containerColor = SurfaceDark) {
                        val tabs = listOf(
                            "Асосӣ" to Icons.Default.Home,
                            "Луғат" to Icons.Default.MenuBook,
                            "Дарсҳо" to Icons.Default.School,
                            "Асбобҳо" to Icons.Default.Build,
                            "Такрор" to Icons.Default.Refresh,
                            "Танзимот" to Icons.Default.Settings
                        )

                        tabs.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        item.second,
                                        contentDescription = item.first
                                    )
                                },
                                label = { Text(item.first) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Purple,
                                    selectedTextColor = Purple,
                                    indicatorColor = Color(0xFF38265B),
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            AnimatedContent(targetState = selectedTab, label = "screen", transitionSpec = { (fadeIn(tween(250)) + slideInHorizontally { it / 12 }) togetherWith (fadeOut(tween(180)) + slideOutHorizontally { -it / 12 }) }) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    language = language,
                    onLanguageChange = { language = it },
                    padding = padding,
                    wordCount = words.size,
                    completedDates = completedDates,
                    learnedCount = learnedCount,
                    correctAnswers = correctAnswers,
                    wrongAnswers = wrongAnswers,
                    onStartStudy = { selectedTab = 2 }
                )

                1 -> DictionaryScreen(
                    words = words,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    padding = padding,
                    favoriteIds = favoriteIds,
                    onFavoriteToggle = { id ->
                        scope.launch {
                            if (id in favoriteIds) {
                                database.favoriteWordDao().removeFavorite(id)
                            } else {
                                database.favoriteWordDao().addFavorite(
                                    FavoriteWordEntity(wordId = id)
                                )
                            }
                        }
                    }
                )

                2 -> StudyScreen(database, padding)

                3 -> V16Screen(database, padding)

                4 -> SmartReviewScreen(database, padding)

                else -> ProfileScreen(
                    language,
                    { language = it },
                    padding
                )
            }
        }
    }
}


}
private fun calculateStreakStats(dates: List<String>): Pair<Int, Int> {
    val studied = dates.mapNotNull {
        runCatching { java.time.LocalDate.parse(it) }.getOrNull()
    }.distinct().sorted()

    if (studied.isEmpty()) return 0 to 0

    var best = 1
    var run = 1

    for (i in 1 until studied.size) {
        run = if (studied[i - 1].plusDays(1) == studied[i]) run + 1 else 1
        best = maxOf(best, run)
    }

    val today = java.time.LocalDate.now()
    var current = 0
    var expected = when {
        today in studied -> today
        today.minusDays(1) in studied -> today.minusDays(1)
        else -> null
    }

    while (expected != null && expected in studied) {
        current++
        expected = expected.minusDays(1)
    }

    return current to best
}

@Composable
private fun StreakCalendarCard(
    completedDates: List<String>,
    currentStreak: Int,
    bestStreak: Int
) {
    val month = java.time.YearMonth.now()
    val studied = remember(completedDates) { completedDates.toSet() }
    val monthNames = listOf(
        "Январ", "Феврал", "Март", "Апрел", "Май", "Июн",
        "Июл", "Август", "Сентябр", "Октябр", "Ноябр", "Декабр"
    )
    val weekdays = listOf("Ду", "Се", "Чо", "Пн", "Ҷм", "Шн", "Як")
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val totalCells = ((firstDayOffset + month.lengthOfMonth() + 6) / 7) * 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Тақвими омӯзиш",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${monthNames[month.monthValue - 1]} ${month.year}",
                    color = Purple,
                    fontWeight = FontWeight.Bold
                )
                Text("🔥 $currentStreak рӯз", color = TextWhite)
            }

            Row {
                weekdays.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, color = Muted, fontSize = 11.sp)
                    }
                }
            }

            (0 until totalCells).toList().chunked(7).forEach { week ->
                Row {
                    week.forEach { cell ->
                        val dayNumber = cell - firstDayOffset + 1
                        Box(
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNumber).toString()
                                val done = date in studied
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = if (done) Purple else Color.Transparent
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "$dayNumber",
                                            color = if (done) Color.White else TextWhite,
                                            fontSize = 12.sp,
                                            fontWeight = if (done) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Рекорди беҳтарин: $bestStreak рӯз", color = Muted, fontSize = 12.sp)
                Text("🟣 Омӯзиш", color = Purple, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HomeScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    padding: PaddingValues,
    wordCount: Int,
    completedDates: List<String>,
    learnedCount: Int,
    correctAnswers: Int,
    wrongAnswers: Int,
    onStartStudy: () -> Unit
) {
    val streakStats = remember(completedDates) {
        calculateStreakStats(completedDates)
    }
    val currentStreak = streakStats.first
    val bestStreak = streakStats.second

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("SONI", color = Purple, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("VOCABULARY", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                LanguageMenu(language, onLanguageChange)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✦ SMART LEARNING",
                            color = Purple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            color = Purple.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                "🔥 $currentStreak рӯз",
                                color = TextWhite,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        "Салом! 👋",
                        color = Muted,
                        fontSize = 15.sp
                    )

                    Text(
                        "Имрӯз як қадам ба пеш!",
                        color = TextWhite,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )

                    Text(
                        "Калимаҳои нав омӯз, дониши худро зиёд кун ва ҳар рӯз пешравӣ намо.",
                        color = Muted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )

                    Button(
                        onClick = onStartStudy,
                        colors = ButtonDefaults.buttonColors(containerColor = Purple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Оғози омӯзиш",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            ProgressStatsCard(
                learnedCount = learnedCount,
                correctAnswers = correctAnswers,
                wrongAnswers = wrongAnswers
            )
        }

        item {
            Text(
                "Натиҷаи омӯзиш",
                color = TextWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("$wordCount", "Калима", Modifier.weight(1f))
                StatCard("$correctAnswers", "Ҷавоби дуруст", Modifier.weight(1f))
                StatCard("$currentStreak", "Рӯзи пайдарпай", Modifier.weight(1f))
            }
        }

        item {
            StreakCalendarCard(
                completedDates = completedDates,
                currentStreak = currentStreak,
                bestStreak = bestStreak
            )
        }

        item {
            Text(
                "Машғулиятҳои ҳаррӯза",
                color = TextWhite,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(3) { index ->
            SessionCard(
                number = index + 1,
                onClick = onStartStudy
            )
        }

        item {
            Text(
                "© 2026 SONI VOCABULARY",
                color = Muted,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = Purple, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun SessionCard(number: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Purple.copy(alpha = 0.18f),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "%02d".format(number),
                        color = Purple,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Машғулияти $number",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Омӯзиши калимаҳо ва ҷумлаҳо",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = Purple.copy(alpha = 0.16f),
                shape = RoundedCornerShape(50.dp)
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Оғоз",
                    tint = Purple,
                    modifier = Modifier.padding(9.dp).size(19.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageMenu(language: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(language, color = TextWhite)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Purple)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("Тоҷикӣ", "Русский", "English").forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DictionaryScreen(
    words: List<WordEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    padding: PaddingValues,
    favoriteIds: Set<String>,
    onFavoriteToggle: (String) -> Unit
) {
    val context = LocalContext.current
    var favoritesOnly by remember { mutableStateOf(false) }

    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Забони англисӣ барои талаффуз
            }
        }
    }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    val visibleWords = if (favoritesOnly) {
        words.filter { it.id in favoriteIds }
    } else {
        words
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp)
    ) {
        Text(
            "Луғат",
            color = TextWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ҷустуҷӯи калима...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ёфт шуд: ${visibleWords.size}",
                color = Muted,
                fontSize = 13.sp
            )

            TextButton(onClick = { favoritesOnly = !favoritesOnly }) {
                Icon(
                    if (favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = Purple
                )
                Text(
                    if (favoritesOnly) "Ҳама" else "Дӯстдоштаҳо",
                    color = Purple
                )
            }
        }

        if (visibleWords.isEmpty()) {
            Text(
                if (favoritesOnly) "Ҳоло калимаи дӯстдошта нест."
                else "Калима ёфт нашуд.",
                color = Muted
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleWords, key = { it.id }) { word ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    word.english,
                                    color = Purple,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = {
                                    tts.language = Locale.US
                                    tts.speak(
                                        word.english,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        word.id
                                    )
                                }) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "Талаффуз",
                                        tint = Purple
                                    )
                                }

                                IconButton(onClick = {
                                    onFavoriteToggle(word.id)
                                }) {
                                    Icon(
                                        if (word.id in favoriteIds)
                                            Icons.Default.Star
                                        else Icons.Default.StarBorder,
                                        contentDescription = "Дӯстдошта",
                                        tint = Purple
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(word.tajik, color = TextWhite, fontSize = 16.sp)
                            Text(word.russian, color = Muted, fontSize = 14.sp)

                            if (word.example.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(word.example, color = Muted, fontSize = 13.sp)
                            }

                            Text(
                                "${word.category} • ${word.level}",
                                color = Purple,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressStatsCard(
    learnedCount: Int,
    correctAnswers: Int,
    wrongAnswers: Int
) {
    val total = correctAnswers + wrongAnswers
    val accuracy = if (total > 0) correctAnswers * 100 / total else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Омори пешрафт",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("$learnedCount", color = Purple, fontSize = 23.sp)
                    Text("Азёдшуда", color = Muted, fontSize = 12.sp)
                }
                Column {
                    Text("$total", color = Purple, fontSize = 23.sp)
                    Text("Ҷавобҳо", color = Muted, fontSize = 12.sp)
                }
                Column {
                    Text("$accuracy%", color = Purple, fontSize = 23.sp)
                    Text("Дуруст", color = Muted, fontSize = 12.sp)
                }
            }

            LinearProgressIndicator(
                progress = { accuracy / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Purple,
                trackColor = Background
            )

            Text(
                "Дуруст: $correctAnswers  •  Нодуруст: $wrongAnswers",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ProfileScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    padding: PaddingValues
) {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    fun openPhoneSettings(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            } catch (_: Exception) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Танзимот",
            color = TextWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Огоҳиҳо ва ёдраскуниҳо",
            color = Purple,
            fontWeight = FontWeight.Bold
        )

        SettingsRow(
            icon = Icons.Default.Notifications,
            title = "Огоҳиҳои барнома",
            subtitle = "Идора кардани иҷозати огоҳиҳо"
        ) {
            openPhoneSettings(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }

        SettingsRow(
            icon = Icons.Default.Alarm,
            title = "Танзими ҳушдорҳо",
            subtitle = "Иҷозатҳо ва танзимоти ҳушдорҳои SONI"
        ) {
            openPhoneSettings(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"))
            )
        }

        HorizontalDivider(color = SurfaceDark)

        Text(
            "Кори барнома дар телефон",
            color = Purple,
            fontWeight = FontWeight.Bold
        )

        SettingsRow(
            icon = Icons.Default.BatteryFull,
            title = "Батарея ва кори пасзамина",
            subtitle = "Танзими маҳдудиятҳои батарея"
        ) {
            openPhoneSettings(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
        }

        SettingsRow(
            icon = Icons.Default.PlayArrow,
            title = "Оғози худкор (Huawei)",
            subtitle = "App launch ва Run in background"
        ) {
            openPhoneSettings(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }

        SettingsRow(
            icon = Icons.Default.Security,
            title = "Иҷозатҳои барнома",
            subtitle = "Идора кардани иҷозатҳои SONI"
        ) {
            openPhoneSettings(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }

        HorizontalDivider(color = SurfaceDark)

        Text(
            "Танзимоти барнома",
            color = Purple,
            fontWeight = FontWeight.Bold
        )

        SettingsRow(
            icon = Icons.Default.Language,
            title = "Забони барнома",
            subtitle = "Забони интерфейсро интихоб кунед"
        ) {
            showLanguageMenu = true
        }

        if (showLanguageMenu) {
            AlertDialog(
                onDismissRequest = { showLanguageMenu = false },
                title = { Text("Забони барнома") },
                text = {
                    Column {
                        listOf("Тоҷикӣ", "Русский", "English").forEach { item ->
                            TextButton(
                                onClick = {
                                    onLanguageChange(item)
                                    showLanguageMenu = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    item,
                                    color = if (language == item) Purple else TextWhite
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageMenu = false }) {
                        Text("Пӯшидан")
                    }
                }
            )
        }

        SettingsRow(
            icon = Icons.Default.Info,
            title = "Дар бораи барнома",
            subtitle = "LEXIO · Версия 1.9"
        ) {
            showAbout = true
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("LEXIO") },
                text = {
                    Text(
                        "LEXIO — Барномаи омӯзиши забонҳо.\n\nВерсия: 1.9\nPackage: tj.soni.vocabulary"
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) {
                        Text("Хуб")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(27.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Muted
            )
        }
    }
}
