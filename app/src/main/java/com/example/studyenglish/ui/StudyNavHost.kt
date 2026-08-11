package com.example.studyenglish.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studyenglish.StudyApp
import com.example.studyenglish.data.StudyRepository
import com.example.studyenglish.ui.screens.CourseListScreen
import com.example.studyenglish.ui.screens.HomeScreen
import com.example.studyenglish.ui.screens.LessonListScreen
import com.example.studyenglish.ui.screens.ListeningScreen
import com.example.studyenglish.ui.screens.QuizScreen
import com.example.studyenglish.ui.screens.ReviewScreen
import com.example.studyenglish.ui.screens.SettingsScreen
import com.example.studyenglish.ui.screens.StatsScreen
import com.example.studyenglish.ui.screens.StudyScreen
import com.example.studyenglish.ui.screens.WordListScreen

/** Application が保持する Repository を取得する */
@Composable
fun rememberRepository(): StudyRepository {
    val context = LocalContext.current
    return (context.applicationContext as StudyApp).repository
}

object Routes {
    const val HOME = "home"
    const val COURSES = "courses"
    const val LESSONS = "lessons/{courseId}/{courseName}"
    const val WORDS = "words/{lessonId}/{lessonTitle}"
    const val STUDY = "study/{lessonId}/{lessonTitle}"
    const val LISTENING = "listening/{sourceType}/{sourceId}/{title}"
    const val QUIZ = "quiz/{lessonId}/{lessonTitle}"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    const val REVIEW = "review/{mode}"

    fun review(mode: String) = "review/$mode"

    fun lessons(courseId: Long, courseName: String) =
        "lessons/$courseId/${Uri.encode(courseName)}"

    fun words(lessonId: Long, lessonTitle: String) =
        "words/$lessonId/${Uri.encode(lessonTitle)}"

    fun study(lessonId: Long, lessonTitle: String) =
        "study/$lessonId/${Uri.encode(lessonTitle)}"

    fun listening(lessonId: Long, lessonTitle: String) =
        "listening/lesson/$lessonId/${Uri.encode(lessonTitle)}"

    fun listeningCourse(courseId: Long, courseName: String) =
        "listening/course/$courseId/${Uri.encode(courseName)}"

    fun quiz(lessonId: Long, lessonTitle: String) =
        "quiz/$lessonId/${Uri.encode(lessonTitle)}"
}

@Composable
fun StudyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCourses = { navController.navigate(Routes.COURSES) },
                // リスニング・クイズもコースから対象レッスンを選んで開始する
                onOpenListening = { navController.navigate(Routes.COURSES) },
                onOpenQuiz = { navController.navigate(Routes.COURSES) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) },
                onOpenWeakReview = { navController.navigate(Routes.review("weak")) },
                onOpenFavoriteReview = { navController.navigate(Routes.review("favorite")) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.REVIEW,
            arguments = listOf(navArgument("mode") { type = NavType.StringType }),
        ) { entry ->
            val mode = entry.arguments?.getString("mode").orEmpty()
            ReviewScreen(mode = mode, onBack = { navController.popBackStack() })
        }
        composable(Routes.COURSES) {
            CourseListScreen(
                onBack = { navController.popBackStack() },
                onCourseClick = { course ->
                    navController.navigate(Routes.lessons(course.id, course.name))
                },
            )
        }
        composable(
            Routes.LESSONS,
            arguments = listOf(
                navArgument("courseId") { type = NavType.LongType },
                navArgument("courseName") { type = NavType.StringType },
            ),
        ) { entry ->
            val courseId = entry.arguments?.getLong("courseId") ?: 0L
            val courseName = entry.arguments?.getString("courseName").orEmpty()
            LessonListScreen(
                courseId = courseId,
                courseName = courseName,
                onBack = { navController.popBackStack() },
                onLessonClick = { lesson ->
                    navController.navigate(Routes.words(lesson.id, lesson.title))
                },
                onListenCourse = {
                    navController.navigate(Routes.listeningCourse(courseId, courseName))
                },
            )
        }
        composable(
            Routes.WORDS,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.LongType },
                navArgument("lessonTitle") { type = NavType.StringType },
            ),
        ) { entry ->
            val lessonId = entry.arguments?.getLong("lessonId") ?: 0L
            val lessonTitle = entry.arguments?.getString("lessonTitle").orEmpty()
            WordListScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onBack = { navController.popBackStack() },
                onStudy = { navController.navigate(Routes.study(lessonId, lessonTitle)) },
                onQuiz = { navController.navigate(Routes.quiz(lessonId, lessonTitle)) },
                onListen = { navController.navigate(Routes.listening(lessonId, lessonTitle)) },
            )
        }
        composable(
            Routes.STUDY,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.LongType },
                navArgument("lessonTitle") { type = NavType.StringType },
            ),
        ) { entry ->
            val lessonId = entry.arguments?.getLong("lessonId") ?: 0L
            val lessonTitle = entry.arguments?.getString("lessonTitle").orEmpty()
            StudyScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.LISTENING,
            arguments = listOf(
                navArgument("sourceType") { type = NavType.StringType },
                navArgument("sourceId") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType },
            ),
        ) { entry ->
            val sourceType = entry.arguments?.getString("sourceType").orEmpty()
            val sourceId = entry.arguments?.getLong("sourceId") ?: 0L
            val title = entry.arguments?.getString("title").orEmpty()
            ListeningScreen(
                sourceKey = "$sourceType:$sourceId",
                title = title,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.QUIZ,
            arguments = listOf(
                navArgument("lessonId") { type = NavType.LongType },
                navArgument("lessonTitle") { type = NavType.StringType },
            ),
        ) { entry ->
            val lessonId = entry.arguments?.getLong("lessonId") ?: 0L
            val lessonTitle = entry.arguments?.getString("lessonTitle").orEmpty()
            QuizScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
