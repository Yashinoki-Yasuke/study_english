package com.example.studyenglish.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.example.studyenglish.ui.ads.BannerAd
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyenglish.data.LearnStats
import com.example.studyenglish.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val repository = rememberRepository()
    val stats by remember { repository.stats() }.collectAsState(initial = LearnStats())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("学習統計") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 連続学習日数 / 今日の学習
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigStatCard(
                    modifier = Modifier.weight(1f),
                    label = "連続学習",
                    value = "${stats.streak}",
                    unit = "日",
                )
                BigStatCard(
                    modifier = Modifier.weight(1f),
                    label = "今日の学習",
                    value = "${stats.todayCount}",
                    unit = "回",
                )
            }

            // 覚えた進捗バー
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("覚えた単語", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val ratio = if (stats.total > 0) stats.learned.toFloat() / stats.total else 0f
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${stats.learned} / ${stats.total} 語（${(ratio * 100).toInt()}%）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // 内訳
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatRow("覚えた", stats.learned, Color(0xFF2E7D32))
                    StatRow("苦手", stats.weak, Color(0xFFC62828))
                    StatRow("未学習", stats.unlearned, MaterialTheme.colorScheme.onSurfaceVariant)
                    StatRow("お気に入り", stats.favorites, Color(0xFFF9A825))
                    StatRow("合計", stats.total, MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun BigStatCard(modifier: Modifier, label: String, value: String, unit: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(unit, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = color, style = MaterialTheme.typography.titleMedium)
        Text("$count 語", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}
