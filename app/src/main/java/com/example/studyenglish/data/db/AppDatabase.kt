package com.example.studyenglish.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [Course::class, Lesson::class, Word::class, Progress::class, StudyLog::class],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun lessonDao(): LessonDao
    abstract fun wordDao(): WordDao
    abstract fun progressDao(): ProgressDao
    abstract fun studyLogDao(): StudyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                val db = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "study_english.db",
                )
                    // 開発中はスキーマ変更時に作り直す（リリース前のため）
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = db
                // コールバック(onCreate)は破壊的マイグレーション時に呼ばれないため、
                // 「データが空なら投入する」方式で確実にseedする
                scope.launch {
                    if (db.courseDao().count() == 0) {
                        SeedData.populate(appContext, db)
                    }
                }
                db
            }
        }
    }
}
