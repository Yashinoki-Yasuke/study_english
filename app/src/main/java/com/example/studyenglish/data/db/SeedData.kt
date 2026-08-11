package com.example.studyenglish.data.db

import android.content.Context
import org.json.JSONObject

/**
 * assets/vocab.json から語彙データを読み込んでDBへ投入する。
 * データ出典:
 *  - 単語: JMdict (Jitendex/JMdict-Yomitan, © EDRDG, CC BY-SA 4.0)
 *  - フレーズ: Tatoeba Project (CC BY 2.0 FR)
 */
object SeedData {

    suspend fun populate(context: Context, db: AppDatabase) {
        if (db.courseDao().count() > 0) return

        val json = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val courses = root.getJSONArray("courses")

        val courseDao = db.courseDao()
        val lessonDao = db.lessonDao()
        val wordDao = db.wordDao()

        for (ci in 0 until courses.length()) {
            val course = courses.getJSONObject(ci)
            val courseId = courseDao.insert(
                Course(
                    name = course.getString("name"),
                    level = ci + 1,
                    description = course.optString("description").ifEmpty { null },
                )
            )
            val lessons = course.getJSONArray("lessons")
            for (li in 0 until lessons.length()) {
                val lesson = lessons.getJSONObject(li)
                val lessonId = lessonDao.insert(
                    Lesson(courseId = courseId, title = lesson.getString("title"), orderIndex = li)
                )
                val words = lesson.getJSONArray("words")
                val toInsert = ArrayList<Word>(words.length())
                for (wi in 0 until words.length()) {
                    val w = words.getJSONObject(wi)
                    toInsert.add(
                        Word(
                            lessonId = lessonId,
                            type = w.optString("type", "word"),
                            english = w.getString("english"),
                            japanese = w.getString("japanese"),
                            phonetic = w.optString("phonetic").ifEmpty { null },
                            example = w.optString("example").ifEmpty { null },
                            orderIndex = wi,
                        )
                    )
                }
                wordDao.insertAll(toInsert)
            }
        }
    }
}
