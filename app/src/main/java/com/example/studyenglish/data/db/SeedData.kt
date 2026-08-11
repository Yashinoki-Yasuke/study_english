package com.example.studyenglish.data.db

/**
 * 動作確認用のサンプルデータ。
 * ※ 実データ（Cambridge 学習者向け辞書を参照した語彙）は別途ライセンス確認のうえ整備する。
 */
object SeedData {

    private data class SeedWord(
        val english: String,
        val japanese: String,
        val phonetic: String? = null,
        val type: String = "word",
    )

    private data class SeedLesson(val title: String, val words: List<SeedWord>)
    private data class SeedCourse(val name: String, val description: String, val lessons: List<SeedLesson>)

    private val courses = listOf(
        SeedCourse(
            name = "小学生レベル",
            description = "英語の基礎。身近な単語から。",
            lessons = listOf(
                SeedLesson(
                    "あいさつ・基本",
                    listOf(
                        SeedWord("hello", "こんにちは", "/həˈloʊ/"),
                        SeedWord("thank you", "ありがとう", "/ˈθæŋk juː/"),
                        SeedWord("apple", "りんご", "/ˈæpəl/"),
                        SeedWord("dog", "犬", "/dɔːɡ/"),
                        SeedWord("cat", "猫", "/kæt/"),
                        SeedWord("book", "本", "/bʊk/"),
                        SeedWord("water", "水", "/ˈwɔːtər/"),
                        SeedWord("friend", "友だち", "/frend/"),
                    ),
                ),
                SeedLesson(
                    "数と色",
                    listOf(
                        SeedWord("one", "1（いち）", "/wʌn/"),
                        SeedWord("two", "2（に）", "/tuː/"),
                        SeedWord("three", "3（さん）", "/θriː/"),
                        SeedWord("red", "赤", "/red/"),
                        SeedWord("blue", "青", "/bluː/"),
                        SeedWord("green", "緑", "/ɡriːn/"),
                    ),
                ),
            ),
        ),
        SeedCourse(
            name = "中学生レベル",
            description = "教科書によく出る単語・動詞。",
            lessons = listOf(
                SeedLesson(
                    "日常の動詞",
                    listOf(
                        SeedWord("study", "勉強する", "/ˈstʌdi/"),
                        SeedWord("listen", "聞く", "/ˈlɪsən/"),
                        SeedWord("write", "書く", "/raɪt/"),
                        SeedWord("speak", "話す", "/spiːk/"),
                        SeedWord("remember", "覚えている", "/rɪˈmembər/"),
                        SeedWord("understand", "理解する", "/ˌʌndərˈstænd/"),
                    ),
                ),
                SeedLesson(
                    "学校生活",
                    listOf(
                        SeedWord("teacher", "先生", "/ˈtiːtʃər/"),
                        SeedWord("subject", "教科", "/ˈsʌbdʒɪkt/"),
                        SeedWord("homework", "宿題", "/ˈhoʊmwɜːrk/"),
                        SeedWord("library", "図書館", "/ˈlaɪbreri/"),
                    ),
                ),
            ),
        ),
        SeedCourse(
            name = "高校生レベル",
            description = "入試・検定でよく問われる語彙。",
            lessons = listOf(
                SeedLesson(
                    "頻出単語",
                    listOf(
                        SeedWord("environment", "環境", "/ɪnˈvaɪrənmənt/"),
                        SeedWord("society", "社会", "/səˈsaɪəti/"),
                        SeedWord("increase", "増加する", "/ɪnˈkriːs/"),
                        SeedWord("experience", "経験", "/ɪkˈspɪriəns/"),
                        SeedWord("develop", "発展させる", "/dɪˈveləp/"),
                    ),
                ),
            ),
        ),
        SeedCourse(
            name = "社会人レベル",
            description = "ビジネスで使う単語・熟語。",
            lessons = listOf(
                SeedLesson(
                    "ビジネス基礎",
                    listOf(
                        SeedWord("schedule", "予定・日程", "/ˈskedʒuːl/"),
                        SeedWord("meeting", "会議", "/ˈmiːtɪŋ/"),
                        SeedWord("deadline", "締め切り", "/ˈdedlaɪn/"),
                        SeedWord("client", "顧客", "/ˈklaɪənt/"),
                        SeedWord("negotiate", "交渉する", "/nɪˈɡoʊʃieɪt/"),
                    ),
                ),
                SeedLesson(
                    "よく使う熟語",
                    listOf(
                        SeedWord("look forward to", "～を楽しみに待つ", type = "idiom"),
                        SeedWord("get in touch", "連絡を取る", type = "idiom"),
                        SeedWord("come up with", "（案などを）思いつく", type = "idiom"),
                        SeedWord("in charge of", "～を担当して", type = "idiom"),
                    ),
                ),
            ),
        ),
    )

    suspend fun populate(db: AppDatabase) {
        if (db.courseDao().count() > 0) return
        val courseDao = db.courseDao()
        val lessonDao = db.lessonDao()
        val wordDao = db.wordDao()

        courses.forEachIndexed { courseIndex, course ->
            val courseId = courseDao.insert(
                Course(name = course.name, level = courseIndex + 1, description = course.description)
            )
            course.lessons.forEachIndexed { lessonIndex, lesson ->
                val lessonId = lessonDao.insert(
                    Lesson(courseId = courseId, title = lesson.title, orderIndex = lessonIndex)
                )
                val words = lesson.words.mapIndexed { wordIndex, w ->
                    Word(
                        lessonId = lessonId,
                        type = w.type,
                        english = w.english,
                        japanese = w.japanese,
                        phonetic = w.phonetic,
                        orderIndex = wordIndex,
                    )
                }
                wordDao.insertAll(words)
            }
        }
    }
}
