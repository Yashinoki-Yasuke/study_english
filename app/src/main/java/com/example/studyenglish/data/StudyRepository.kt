package com.example.studyenglish.data

import com.example.studyenglish.data.db.AppDatabase
import com.example.studyenglish.data.db.Course
import com.example.studyenglish.data.db.Lesson
import com.example.studyenglish.data.db.Progress
import com.example.studyenglish.data.db.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** CSVインポート結果（追加件数・不正行のスキップ件数） */
data class CsvImportResult(val added: Int, val skipped: Int)

/** 学習統計 */
data class LearnStats(
    val total: Int = 0,
    val learned: Int = 0,
    val weak: Int = 0,
    val favorites: Int = 0,
    val streak: Int = 0,
    val todayCount: Int = 0,
) {
    val unlearned: Int get() = (total - learned - weak).coerceAtLeast(0)
}

/** DBアクセスを集約するリポジトリ */
class StudyRepository(private val db: AppDatabase) {

    fun courses(): Flow<List<Course>> = db.courseDao().getAllCourses()

    /** ユーザーが作成したオリジナル単語帳一覧 */
    fun customCourses(): Flow<List<Course>> = db.courseDao().getCustomCourses()

    /** オリジナル単語帳（コース）に含まれる単語数 */
    fun customWordCount(courseId: Long): Flow<Int> = db.wordDao().wordCountByCourse(courseId)

    /**
     * 新規オリジナル単語帳を作成する。単語帳＝コース1件＋レッスン1件として作成し、
     * 単語を追加する先となるレッスンIDを返す。
     */
    suspend fun createWordbook(name: String): Long {
        val courseId = db.courseDao().insert(Course(name = name, level = 0, isCustom = true))
        return db.lessonDao().insert(Lesson(courseId = courseId, title = name, orderIndex = 0))
    }

    /** オリジナル単語帳（コース）の格納先レッスンIDを取得する */
    suspend fun wordbookLessonId(courseId: Long): Long? =
        db.lessonDao().getFirstLessonByCourseOnce(courseId)?.id

    /** オリジナル単語帳を削除する（進捗の孤立レコードも合わせて削除） */
    suspend fun deleteWordbook(courseId: Long) {
        db.progressDao().deleteByCourse(courseId)
        db.courseDao().delete(courseId)
    }

    /** CSVの各行（生テキスト）をパースして、指定レッスンに単語を追加する */
    suspend fun importWordsFromCsv(lessonId: Long, lines: List<String>): CsvImportResult {
        val existingCount = db.wordDao().getWordsByLessonOnce(lessonId).size
        val parsed = CsvParser.parseWords(lines)
        if (parsed.rows.isNotEmpty()) {
            val words = parsed.rows.mapIndexed { index, row ->
                Word(
                    lessonId = lessonId,
                    type = "word",
                    english = row.english,
                    japanese = row.japanese,
                    phonetic = row.phonetic,
                    example = row.example,
                    orderIndex = existingCount + index,
                )
            }
            db.wordDao().insertAll(words)
        }
        return CsvImportResult(added = parsed.rows.size, skipped = parsed.skipped)
    }

    fun lessons(courseId: Long): Flow<List<Lesson>> =
        db.lessonDao().getLessonsByCourse(courseId)

    fun words(lessonId: Long): Flow<List<Word>> =
        db.wordDao().getWordsByLesson(lessonId)

    suspend fun wordsOnce(lessonId: Long): List<Word> =
        db.wordDao().getWordsByLessonOnce(lessonId)

    suspend fun wordsByCourseOnce(courseId: Long): List<Word> =
        db.wordDao().getWordsByCourseOnce(courseId)

    fun progress(wordId: Long): Flow<Progress?> =
        db.progressDao().getProgress(wordId)

    fun weakWords(): Flow<List<Word>> = db.wordDao().weakWords()

    fun favoriteWords(): Flow<List<Word>> = db.wordDao().favoriteWords()

    suspend fun weakWordsOnce(): List<Word> = db.wordDao().weakWordsOnce()

    suspend fun favoriteWordsOnce(): List<Word> = db.wordDao().favoriteWordsOnce()

    /** 学習状態を保存する（0:未学習 1:覚えた 2:苦手）。お気に入りは保持し、学習ログも記録 */
    suspend fun setWordStatus(wordId: Long, status: Int) {
        val dao = db.progressDao()
        dao.insertIfAbsent(Progress(wordId = wordId))
        dao.updateStatus(wordId, status, System.currentTimeMillis())
        db.studyLogDao().increment(today())
    }

    /** お気に入りフラグを設定する（進捗行が無ければ作成） */
    suspend fun setFavorite(wordId: Long, favorite: Boolean) {
        val dao = db.progressDao()
        dao.insertIfAbsent(Progress(wordId = wordId))
        dao.updateFavorite(wordId, favorite)
    }

    /** 学習統計（件数・お気に入り・連続日数・今日の学習数） */
    fun stats(): Flow<LearnStats> {
        val total = db.wordDao().totalCount()
        val learned = db.progressDao().learnedCount()
        val weak = db.progressDao().weakCount()
        val favorites = db.progressDao().favoriteCount()
        val streak = db.studyLogDao().studyDates().map { calcStreak(it) }
        val todayCount = db.studyLogDao().countForDate(today()).map { it ?: 0 }
        return combine(
            total, learned, weak, favorites, streak, todayCount,
        ) { values ->
            LearnStats(
                total = values[0] as Int,
                learned = values[1] as Int,
                weak = values[2] as Int,
                favorites = values[3] as Int,
                streak = values[4] as Int,
                todayCount = values[5] as Int,
            )
        }
    }

    private fun today(): String = dateFormat.format(Calendar.getInstance().time)

    /** 今日（または昨日）から連続している学習日数を数える */
    private fun calcStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val set = dates.toHashSet()
        val cal = Calendar.getInstance()
        // 今日まだ学習していなければ、昨日から数え始める
        if (!set.contains(dateFormat.format(cal.time))) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
            if (!set.contains(dateFormat.format(cal.time))) return 0
        }
        var streak = 0
        while (set.contains(dateFormat.format(cal.time))) {
            streak++
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return streak
    }

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
