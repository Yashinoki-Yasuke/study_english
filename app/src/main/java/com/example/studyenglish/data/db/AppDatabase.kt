package com.example.studyenglish.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [Course::class, Lesson::class, Word::class, Progress::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun wordDao(): WordDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_english.db",
                )
                    // 開発中はスキーマ変更時に作り直す（リリース前のため）
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 初回作成時に語彙データ(assets/vocab.json)を投入
                            scope.launch {
                                INSTANCE?.let { SeedData.populate(appContext, it) }
                            }
                        }
                    })
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
