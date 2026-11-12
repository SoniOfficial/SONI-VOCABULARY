package tj.soni.vocabulary.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WordEntity::class,
        SentenceEntity::class,
        WordProgressEntity::class,
        DailySessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun sentenceDao(): SentenceDao
    abstract fun wordProgressDao(): WordProgressDao
    abstract fun dailySessionDao(): DailySessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS word_progress (
                        wordId TEXT NOT NULL PRIMARY KEY,
                        correctCount INTEGER NOT NULL DEFAULT 0,
                        wrongCount INTEGER NOT NULL DEFAULT 0,
                        lastReviewedAt INTEGER NOT NULL DEFAULT 0,
                        isLearned INTEGER NOT NULL DEFAULT 0
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_sessions (
                        sessionDate TEXT NOT NULL,
                        sessionNumber INTEGER NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(sessionDate, sessionNumber)
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soni_vocabulary_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
