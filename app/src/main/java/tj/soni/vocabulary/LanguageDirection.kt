package tj.soni.vocabulary

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import tj.soni.vocabulary.data.database.WordEntity

data class LanguageDirection(
    val source: String = "en",
    val target: String = "tg"
)

val lexioLanguages = listOf(
    "tg" to "🇹🇯 Тоҷикӣ",
    "en" to "🇬🇧 English",
    "ru" to "🇷🇺 Русский",
    "zh" to "🇨🇳 中文"
)

fun languageName(code: String): String =
    lexioLanguages.firstOrNull { it.first == code }?.second ?: code

fun wordLanguageText(word: WordEntity, language: String): String =
    when (language) {
        "en" -> word.english
        "tg" -> word.tajik
        "ru" -> word.russian
        "zh" -> listOf(
            word.chinese,
            word.chinesePinyin.takeIf { it.isNotBlank() }?.let { "Pinyin: $it" },
            word.chinesePronunciation.takeIf { it.isNotBlank() }?.let { "Талаффуз: $it" }
        ).filterNotNull().joinToString("\n")
        else -> word.tajik
    }

fun wordSpeechText(word: WordEntity, language: String): String =
    if (language == "zh") word.chinese else wordLanguageText(word, language)

@Composable
fun rememberLanguageDirection(): MutableState<LanguageDirection> {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("lexio_language_preferences", 0)
    }
    return remember {
        mutableStateOf(
            LanguageDirection(
                source = preferences.getString("source", "en") ?: "en",
                target = preferences.getString("target", "tg") ?: "tg"
            )
        )
    }
}

@Composable
fun LanguageDirectionSelector(
    direction: LanguageDirection,
    onChange: (LanguageDirection) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Row {
        TextButton(onClick = { expanded = true }) {
            Text(
                "${languageName(direction.source)}  →  ${languageName(direction.target)}",
                color = Color(0xFF9B72FF)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            lexioLanguages.forEach { source ->
                lexioLanguages.filter { it.first != source.first }.forEach { target ->
                    DropdownMenuItem(
                        text = { Text("${source.second} → ${target.second}") },
                        onClick = {
                            val selected = LanguageDirection(source.first, target.first)
                            context.getSharedPreferences("lexio_language_preferences", 0)
                                .edit()
                                .putString("source", selected.source)
                                .putString("target", selected.target)
                                .apply()
                            onChange(selected)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
