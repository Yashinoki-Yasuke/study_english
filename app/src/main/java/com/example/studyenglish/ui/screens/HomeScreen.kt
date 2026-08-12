package com.example.studyenglish.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.studyenglish.ui.ads.BannerAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCourses: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenWeakReview: () -> Unit,
    onOpenFavoriteReview: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ながら学習「キクミル」") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                },
            )
        },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "今日も英語を学習しましょう！",
                style = MaterialTheme.typography.titleMedium
            )
            MenuCard(
                icon = Icons.Filled.Book,
                title = "単語・熟語学習",
                subtitle = "コース別に単語を学ぶ",
                onClick = onOpenCourses,
            )
            MenuCard(
                icon = Icons.Filled.Headset,
                title = "発音リスニング",
                subtitle = "英語→日本語を交互に再生（ながら学習）",
                onClick = onOpenListening,
            )
            MenuCard(
                icon = Icons.Filled.Quiz,
                title = "クイズ",
                subtitle = "理解度をチェック",
                onClick = onOpenQuiz,
            )
            MenuCard(
                icon = Icons.Filled.Whatshot,
                title = "苦手を復習",
                subtitle = "「苦手」にした単語をまとめて復習",
                onClick = onOpenWeakReview,
            )
            MenuCard(
                icon = Icons.Filled.Star,
                title = "お気に入りを復習",
                subtitle = "★を付けた単語をまとめて復習",
                onClick = onOpenFavoriteReview,
            )
            MenuCard(
                icon = Icons.Filled.BarChart,
                title = "学習統計",
                subtitle = "連続日数・覚えた数などを確認",
                onClick = onOpenStats,
            )
        }
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
