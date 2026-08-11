package com.example.studyenglish.data

import com.example.studyenglish.data.db.AppDatabase
import com.example.studyenglish.data.db.Course
import com.example.studyenglish.data.db.Lesson
import com.example.studyenglish.data.db.Progress
import com.example.studyenglish.data.db.Word
import kotlinx.coroutines.flow.Flow

/** DBアクセスを集約するリポジトリ */
class StudyRepository(private val db: AppDatabase) {

    fun courses(): Flow<List<Course>> = db.courseDao().getAllCourses()

    fun lessons(courseId: Long): Flow<List<Lesson>> =
        db.lessonDao().getLessonsByCourse(courseId)

    fun words(lessonId: Long): Flow<List<Word>> =
        db.wordDao().getWordsByLesson(lessonId)

    suspend fun wordsOnce(lessonId: Long): List<Word> =
        db.wordDao().getWordsByLessonOnce(lessonId)

    fun progress(wordId: Long): Flow<Progress?> =
        db.progressDao().getProgress(wordId)

    /** 学習状態を保存する（0:未学習 1:覚えた 2:苦手） */
    suspend fun setWordStatus(wordId: Long, status: Int) {
        db.progressDao().upsert(
            Progress(
                wordId = wordId,
                status = status,
                lastStudiedAt = System.currentTimeMillis(),
            )
        )
    }
}
