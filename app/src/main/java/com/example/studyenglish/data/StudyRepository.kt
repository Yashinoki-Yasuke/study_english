package com.example.studyenglish.data

import com.example.studyenglish.data.db.AppDatabase
import com.example.studyenglish.data.db.Course
import com.example.studyenglish.data.db.Lesson
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
}
