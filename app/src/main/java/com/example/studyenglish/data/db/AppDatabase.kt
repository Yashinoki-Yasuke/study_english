package com.example.studyenglish.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** version9→10: courses に isCustom（オリジナル単語帳フラグ）を追加 */
private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE courses ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * バージョン9はストア公開時点の基準スキーマ（app/schemas/.../9.json）。
 *
 * 【重要】公開後に version を上げるときは、必ず Migration(oldVersion, newVersion) を
 * 実装して build() に .addMigrations(...) で追加すること。Migration を用意せずに
 * バージョンだけ上げると、fallbackToDestructiveMigration() によって
 * 既存ユーザーの学習データ（進捗・お気に入り・統計）が全消去される。
 *
 * 【内蔵語彙データ(vocab.json)を将来更新する場合の注意】
 * courses/lessons/words はオリジナル単語帳（Course.isCustom=true）と
 * テーブルを共有している。IDはSQLiteの自動採番のためオリジナル単語帳の
 * 単語と衝突することは無いが、以下は必ず守ること:
 *  - 削除/更新のSQLは必ず `WHERE courseId IN (SELECT id FROM courses WHERE isCustom = 0)`
 *    等で内蔵データのみに絞る（絞り忘れるとユーザーのオリジナル単語帳を巻き込んで消す）。
 *  - 既存の内蔵単語の翻訳修正等は、削除→再挿入ではなく UPDATE 文で同じ id の行を
 *    書き換えること。削除→再挿入すると id が変わり、その単語に紐づく学習進捗
 *    （覚えた/苦手/お気に入り）が孤立・リセットされてしまう。
 */
@Database(
    entities = [Course::class, Lesson::class, Word::class, Progress::class, StudyLog::class],
    version = 10,
    exportSchema = true,
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
                    .addMigrations(MIGRATION_9_10)
                    // Migration未実装のバージョン間遷移が発生した場合の最終手段としてのみ機能する
                    // （通常は上記の Migration を正しく追加していれば呼ばれない）。
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
