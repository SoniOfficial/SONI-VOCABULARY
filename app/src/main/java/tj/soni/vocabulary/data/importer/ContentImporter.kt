package tj.soni.vocabulary.data.importer

import android.content.Context
import androidx.room.withTransaction
import tj.soni.vocabulary.data.database.AppDatabase
import tj.soni.vocabulary.data.database.WordEntity
import tj.soni.vocabulary.data.database.SentenceEntity
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(
    val imported: Int,
    val failed: Int,
    val message: String
)

class ContentImporter(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun importWordsFromAssets(fileName: String): ImportResult {
        return try {
            val json = context.assets.open(fileName)
                .bufferedReader()
                .use { it.readText() }

            importWords(json)
        } catch (e: Exception) {
            ImportResult(0, 1, "Word import failed: ${e.message}")
        }
    }

    suspend fun importSentencesFromAssets(fileName: String): ImportResult {
        return try {
            val json = context.assets.open(fileName)
                .bufferedReader()
                .use { it.readText() }

            importSentences(json)
        } catch (e: Exception) {
            ImportResult(0, 1, "Sentence import failed: ${e.message}")
        }
    }

    suspend fun importWords(jsonText: String): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray(jsonText)
                var imported = 0
                var failed = 0

                val dao = database.wordDao()

                var batch = mutableListOf<WordEntity>()

                for (i in 0 until array.length()) {
                    try {
                        val item = array.getJSONObject(i)

                        val id = item.optString("id").trim()
                        val english = item.optString("english").trim()
                        val tajik = item.optString("tajik").trim()
                        val russian = item.optString("russian").trim()
                        val chinese = item.optString("chinese").trim()
                        val chinesePinyin = item.optString("chinesePinyin").trim()
                        val chinesePronunciation = item.optString("chinesePronunciation").trim()

                        if (id.isBlank() || listOf(english, tajik, russian, chinese).all { it.isBlank() }) {
                            failed++
                            continue
                        }

                        batch.add(
                            WordEntity(
                                id = id,
                                english = english,
                                tajik = tajik,
                                russian = russian,
                                chinese = chinese,
                                chinesePinyin = chinesePinyin,
                                chinesePronunciation = chinesePronunciation,
                                category = item.optString("category", "General"),
                                level = item.optString("level", "Beginner"),
                                example = item.optString("example", "")
                            )
                        )

                        if (batch.size >= 500) {
                            dao.upsertAll(batch)
                            imported += batch.size
                            batch = mutableListOf()
                        }
                    } catch (_: Exception) {
                        failed++
                    }
                }

                if (batch.isNotEmpty()) {
                    dao.upsertAll(batch)
                    imported += batch.size
                }

                ImportResult(
                    imported,
                    failed,
                    "Words processed successfully"
                )
            } catch (e: Exception) {
                ImportResult(0, 1, "Invalid JSON: ${e.message}")
            }
        }

    suspend fun importSentences(jsonText: String): ImportResult =
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray(jsonText)
                var imported = 0
                var failed = 0

                val dao = database.sentenceDao()
                var batch = mutableListOf<SentenceEntity>()

                for (i in 0 until array.length()) {
                    try {
                        val item = array.getJSONObject(i)

                        val id = item.optString("id").trim()
                        val english = item.optString("english").trim()
                        val tajik = item.optString("tajik").trim()
                        val russian = item.optString("russian").trim()
                        val chinese = item.optString("chinese").trim()
                        val chinesePinyin = item.optString("chinesePinyin").trim()
                        val chinesePronunciation = item.optString("chinesePronunciation").trim()

                        if (id.isBlank() || listOf(english, tajik, russian, chinese).all { it.isBlank() }) {
                            failed++
                            continue
                        }

                        batch.add(
                            SentenceEntity(
                                id = id,
                                english = english,
                                tajik = tajik,
                                russian = russian,
                                chinese = chinese,
                                chinesePinyin = chinesePinyin,
                                chinesePronunciation = chinesePronunciation,
                                level = item.optString("level", "Beginner"),
                                category = item.optString("category", "General")
                            )
                        )

                        if (batch.size >= 500) {
                            dao.upsertAll(batch)
                            imported += batch.size
                            batch = mutableListOf()
                        }
                    } catch (_: Exception) {
                        failed++
                    }
                }

                if (batch.isNotEmpty()) {
                    dao.upsertAll(batch)
                    imported += batch.size
                }

                ImportResult(
                    imported,
                    failed,
                    "Sentences processed successfully"
                )
            } catch (e: Exception) {
                ImportResult(0, 1, "Invalid JSON: ${e.message}")
            }
        }
}
