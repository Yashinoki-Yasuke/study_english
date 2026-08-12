package com.example.studyenglish.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 学習コース（レベル別）。例：中学生レベル */
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** 並び順（レベルの低い順） */
    val level: Int,
    val description: String? = null,
    /** ユーザーがCSV等で作成したオリジナル単語帳かどうか */
    val isCustom: Boolean = false,
)

/** レッスン（コース内の単語グループ） */
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("courseId")],
)
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val title: String,
    val orderIndex: Int,
)

/** 単語・熟語 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lessonId")],
)
data class Word(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lessonId: Long,
    /** "word"（単語）または "idiom"（熟語） */
    val type: String,
    val english: String,
    val japanese: String,
    val phonetic: String? = null,
    val example: String? = null,
    val orderIndex: Int,
)

/** 学習進捗（単語ごと） */
@Entity(tableName = "progress")
data class Progress(
    @PrimaryKey val wordId: Long,
    /** 0:未学習 1:学習済み 2:苦手 */
    val status: Int = 0,
    val isFavorite: Boolean = false,
    val lastStudiedAt: Long? = null,
)

/** 学習ログ（連続学習日数などの統計用） */
@Entity(tableName = "study_log")
data class StudyLog(
    /** yyyy-MM-dd */
    @PrimaryKey val date: String,
    val studiedCount: Int = 0,
)
