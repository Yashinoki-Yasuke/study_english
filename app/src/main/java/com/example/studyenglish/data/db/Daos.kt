package com.example.studyenglish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses WHERE isCustom = 0 ORDER BY level ASC")
    fun getAllCourses(): Flow<List<Course>>

    /** ユーザー作成のオリジナル単語帳 */
    @Query("SELECT * FROM courses WHERE isCustom = 1 ORDER BY id DESC")
    fun getCustomCourses(): Flow<List<Course>>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int

    @Insert
    suspend fun insert(course: Course): Long

    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun delete(courseId: Long)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getLessonsByCourse(courseId: Long): Flow<List<Lesson>>

    /** オリジナル単語帳は1コース1レッスンで運用するため、先頭のレッスンを取得する */
    @Query("SELECT * FROM lessons WHERE courseId = :courseId LIMIT 1")
    suspend fun getFirstLessonByCourseOnce(courseId: Long): Lesson?

    @Insert
    suspend fun insert(lesson: Lesson): Long
}

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    fun getWordsByLesson(lessonId: Long): Flow<List<Word>>

    /** リスニング再生など、一度だけ取得したい場合に使用 */
    @Query("SELECT * FROM words WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    suspend fun getWordsByLessonOnce(lessonId: Long): List<Word>

    /** コース全体の単語を、レッスン順・単語順で取得 */
    @Query(
        "SELECT w.* FROM words w INNER JOIN lessons l ON w.lessonId = l.id " +
            "WHERE l.courseId = :courseId ORDER BY l.orderIndex ASC, w.orderIndex ASC"
    )
    suspend fun getWordsByCourseOnce(courseId: Long): List<Word>

    @Query("SELECT COUNT(*) FROM words")
    fun totalCount(): Flow<Int>

    /** オリジナル単語帳（コース）に含まれる単語数 */
    @Query(
        "SELECT COUNT(*) FROM words w INNER JOIN lessons l ON w.lessonId = l.id " +
            "WHERE l.courseId = :courseId"
    )
    fun wordCountByCourse(courseId: Long): Flow<Int>

    /** 苦手(status=2)の単語をまとめて取得 */
    @Query(
        "SELECT w.* FROM words w INNER JOIN progress p ON p.wordId = w.id " +
            "WHERE p.status = 2 ORDER BY p.lastStudiedAt DESC"
    )
    fun weakWords(): Flow<List<Word>>

    /** お気に入りの単語をまとめて取得 */
    @Query(
        "SELECT w.* FROM words w INNER JOIN progress p ON p.wordId = w.id " +
            "WHERE p.isFavorite = 1 ORDER BY p.lastStudiedAt DESC"
    )
    fun favoriteWords(): Flow<List<Word>>

    @Query(
        "SELECT w.* FROM words w INNER JOIN progress p ON p.wordId = w.id " +
            "WHERE p.status = 2 ORDER BY p.lastStudiedAt DESC"
    )
    suspend fun weakWordsOnce(): List<Word>

    @Query(
        "SELECT w.* FROM words w INNER JOIN progress p ON p.wordId = w.id " +
            "WHERE p.isFavorite = 1 ORDER BY p.lastStudiedAt DESC"
    )
    suspend fun favoriteWordsOnce(): List<Word>

    @Insert
    suspend fun insert(word: Word): Long

    @Insert
    suspend fun insertAll(words: List<Word>)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    fun getProgress(wordId: Long): Flow<Progress?>

    @Query("SELECT COUNT(*) FROM progress WHERE status = 1")
    fun learnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM progress WHERE status = 2")
    fun weakCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM progress WHERE isFavorite = 1")
    fun favoriteCount(): Flow<Int>

    /** 進捗行が無ければ作成（既存は変更しない） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(progress: Progress)

    @Query("UPDATE progress SET status = :status, lastStudiedAt = :timestamp WHERE wordId = :wordId")
    suspend fun updateStatus(wordId: Long, status: Int, timestamp: Long)

    @Query("UPDATE progress SET isFavorite = :favorite WHERE wordId = :wordId")
    suspend fun updateFavorite(wordId: Long, favorite: Boolean)

    /** コース削除前に呼び、そのコースの単語に紐づく進捗を削除する（孤立レコード防止） */
    @Query(
        "DELETE FROM progress WHERE wordId IN (" +
            "SELECT w.id FROM words w INNER JOIN lessons l ON w.lessonId = l.id " +
            "WHERE l.courseId = :courseId)"
    )
    suspend fun deleteByCourse(courseId: Long)
}

@Dao
interface StudyLogDao {
    /** その日の学習数を+1（無ければ作成） */
    @Query(
        "INSERT INTO study_log(date, studiedCount) VALUES(:date, 1) " +
            "ON CONFLICT(date) DO UPDATE SET studiedCount = studiedCount + 1"
    )
    suspend fun increment(date: String)

    @Query("SELECT date FROM study_log ORDER BY date DESC")
    fun studyDates(): Flow<List<String>>

    @Query("SELECT studiedCount FROM study_log WHERE date = :date")
    fun countForDate(date: String): Flow<Int?>
}
