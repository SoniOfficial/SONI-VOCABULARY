package tj.soni.vocabulary

import android.os.Bundle
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
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.WordEntity
import tj.soni.vocabulary.data.importer.ContentImporter

private val Background = Color(0xFF100B20)
private val SurfaceDark = Color(0xFF1D1533)
private val Purple = Color(0xFF9B72FF)
private val TextWhite = Color(0xFFF8F5FF)
private val Muted = Color(0xFFB8ADC9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VocabularyApp()
        }
    }
}

@Composable
fun VocabularyApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val importer = remember { ContentImporter(context, database) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf("Тоҷикӣ") }
    var searchQuery by remember { mutableStateOf("") }
    var words by remember { mutableStateOf(emptyList<WordEntity>()) }

    // Импорт танҳо ҳангоми тағйир ёфтани версияи контент иҷро мешавад.
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

    // Ҷустуҷӯи зинда аз Room Database.
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
                NavigationBar(containerColor = SurfaceDark) {
                    val tabs = listOf(
                        "Асосӣ" to Icons.Default.Home,
                        "Луғат" to Icons.Default.MenuBook,
                        "Дарсҳо" to Icons.Default.School,
                        "Профил" to Icons.Default.Person
                    )

                    tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(item.second, contentDescription = item.first)
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
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(language, { language = it }, padding, words.size)
                1 -> DictionaryScreen(
                    words = words,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    padding = padding
                )
                2 -> StudyScreen(database, padding)
                else -> ProfileScreen(language, { language = it }, padding)
            }
        }
    }
}

@Composable
fun HomeScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    padding: PaddingValues,
    wordCount: Int
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

        items(5) { index ->
            SessionCard(index + 1)
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
fun SessionCard(number: Int) {
    Card(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Профил",
            color = TextWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
            Column(Modifier.padding(20.dp)) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Донишҷӯи SONI",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Омӯзиши забонҳо", color = Muted)
            }
        }

        Text("Забони барнома", color = TextWhite, fontWeight = FontWeight.Bold)
        LanguageMenu(language, onLanguageChange)
        Text("Версия: 1.0.0", color = Muted)
    }
}
