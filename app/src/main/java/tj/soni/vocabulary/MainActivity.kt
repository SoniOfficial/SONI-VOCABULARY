package tj.soni.vocabulary

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.delay
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.StudyAlarmManager
import tj.soni.vocabulary.data.database.StudyScheduleEntity
import tj.soni.vocabulary.data.database.WordEntity
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

        setContent {
            VocabularyApp()
        }
    }
}

@Composable
fun VocabularyApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }

    val scheduleFlow = remember(database) {
        database.studyScheduleDao().observeAll()
    }
    val schedules by scheduleFlow.collectAsState(initial = emptyList())

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
            when (selectedTab) {
                0 -> HomeScreen(
                    language = language,
                    onLanguageChange = { language = it },
                    padding = padding,
                    wordCount = words.size,
                    onStartStudy = { selectedTab = 2 }
                )

                1 -> DictionaryScreen(
                    words = words,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    padding = padding
                )

                2 -> StudyScreen(database, padding)

                else -> ProfileScreen(
                    language,
                    { language = it },
                    padding
                )
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
    onStartStudy: () -> Unit
) {
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
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Салом! 👋", color = Muted, fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Имрӯз забони нав омӯз!",
                        color = TextWhite,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Ҳар рӯз каме омӯз — натиҷаи калон мегирӣ.",
                        color = Muted
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = Purple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Оғози омӯзиш", color = Color.White)
                    }
                }
            }
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
                StatCard("0", "Ҷумла", Modifier.weight(1f))
                StatCard("0", "Рӯз", Modifier.weight(1f))
            }
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
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Purple)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Машғулияти $number", color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Омӯзиши калимаҳо ва ҷумлаҳо", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted)
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
    padding: PaddingValues
) {
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

        Spacer(Modifier.height(12.dp))

        Text(
            "Ёфт шуд: ${words.size}",
            color = Muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(8.dp))

        if (words.isEmpty()) {
            Text(
                "Калима ёфт нашуд. Баъдтар файлҳои навро ворид кун.",
                color = Muted
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(words, key = { it.id }) { word ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                word.english,
                                color = Purple,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(word.tajik, color = TextWhite, fontSize = 16.sp)
                            Text(word.russian, color = Muted, fontSize = 14.sp)
                            if (word.example.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    word.example,
                                    color = Muted,
                                    fontSize = 13.sp
                                )
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
fun LessonsScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Дарсҳо",
            color = TextWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        listOf(
            "Beginner — Оғозӣ",
            "Intermediate — Миёна",
            "Advanced — Пешрафта"
        ).forEach {
            Card(
                modifier = Modifier.fillMaxWidth().clickable {},
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Purple)
                    Spacer(Modifier.width(14.dp))
                    Text(it, color = TextWhite, fontWeight = FontWeight.Bold)
                }
            }
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
            subtitle = "SONI VOCABULARY · Версия 1.3"
        ) {
            showAbout = true
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                title = { Text("SONI VOCABULARY") },
                text = {
                    Text(
                        "Барномаи омӯзиши забонҳо.\n\nВерсия: 1.3\nPackage: tj.soni.vocabulary"
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
