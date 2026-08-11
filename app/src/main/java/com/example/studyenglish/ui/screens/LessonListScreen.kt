package com.example.studyenglish.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.example.studyenglish.ui.ads.BannerAd
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyenglish.data.db.Lesson
import com.example.studyenglish.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    courseId: Long,
    courseName: String,
    onBack: () -> Unit,
    onLessonClick: (Lesson) -> Unit,
    onListenCourse: () -> Unit,
) {
    val repository = rememberRepository()
    val lessonsFlow = remember(courseId) { repository.lessons(courseId) }
    val lessons by lessonsFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(courseName.ifEmpty { "レッスン" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            item {
                // コース全体をまとめて発音リスニング
                Button(
                    onClick = onListenCourse,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Icon(Icons.Filled.Headset, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("コース全体を聞く（発音リスニング）")
                }
            }
            items(lessons) { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { onLessonClick(lesson) },
                ) {
                    Text(
                        lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
