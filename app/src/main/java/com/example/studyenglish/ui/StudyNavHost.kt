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
import com.example.studyenglish.ui.screens.PlaceholderScreen
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
    const val LISTENING = "listening/{lessonId}/{lessonTitle}"
    const val QUIZ = "quiz"

    fun lessons(courseId: Long, courseName: String) =
        "lessons/$courseId/${Uri.encode(courseName)}"

    fun words(lessonId: Long, lessonTitle: String) =
        "words/$lessonId/${Uri.encode(lessonTitle)}"

    fun study(lessonId: Long, lessonTitle: String) =
        "study/$lessonId/${Uri.encode(lessonTitle)}"

    fun listening(lessonId: Long, lessonTitle: String) =
        "listening/$lessonId/${Uri.encode(lessonTitle)}"
}

@Composable
fun StudyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCourses = { navController.navigate(Routes.COURSES) },
                // リスニングもコースから対象レッスンを選んで開始する
                onOpenListening = { navController.navigate(Routes.COURSES) },
                onOpenQuiz = { navController.navigate(Routes.QUIZ) },
            )
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
                navArgument("lessonId") { type = NavType.LongType },
                navArgument("lessonTitle") { type = NavType.StringType },
            ),
        ) { entry ->
            val lessonId = entry.arguments?.getLong("lessonId") ?: 0L
            val lessonTitle = entry.arguments?.getString("lessonTitle").orEmpty()
            ListeningScreen(
                lessonId = lessonId,
                lessonTitle = lessonTitle,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.QUIZ) {
            PlaceholderScreen(title = "クイズ", onBack = { navController.popBackStack() })
        }
    }
}
