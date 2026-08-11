package com.example.studyenglish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY level ASC")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT COUNT(*) FROM courses")
    suspend fun count(): Int

    @Insert
    suspend fun insert(course: Course): Long
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getLessonsByCourse(courseId: Long): Flow<List<Lesson>>

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

    @Insert
    suspend fun insert(word: Word): Long

    @Insert
    suspend fun insertAll(words: List<Word>)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE wordId = :wordId")
    fun getProgress(wordId: Long): Flow<Progress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: Progress)
}
