package com.example.studyenglish.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyenglish.audio.WordSpeaker
import com.example.studyenglish.ui.rememberRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    lessonId: Long,
    lessonTitle: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = rememberRepository()
    val scope = rememberCoroutineScope()
    val wordsFlow = remember(lessonId) { repository.words(lessonId) }
    val words by wordsFlow.collectAsState(initial = emptyList())

    val speaker = remember { WordSpeaker(context) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }

    var index by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle.ifEmpty { "学習" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (words.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text("単語がありません") }
            return@Scaffold
        }

        val safeIndex = index.coerceIn(0, words.size - 1)
        val word = words[safeIndex]

        fun goTo(newIndex: Int) {
            index = newIndex.coerceIn(0, words.size - 1)
            showAnswer = false
        }

        fun mark(status: Int) {
            scope.launch { repository.setWordStatus(word.id, status) }
            if (safeIndex < words.size - 1) goTo(safeIndex + 1)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { (safeIndex + 1f) / words.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${safeIndex + 1} / ${words.size}",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(16.dp))

            // 学習カード（タップで意味の表示切替）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAnswer = !showAnswer },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (word.type == "idiom") "熟語" else "単語",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = word.english,
                        fontSize = 34.sp,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    word.phonetic?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (showAnswer) {
                        Text(
                            text = word.japanese,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        word.example?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    } else {
                        TextButton(onClick = { showAnswer = true }) {
                            Text("タップして意味を表示")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { speaker.speakEnglish(word.english) }) {
                Icon(Icons.Filled.VolumeUp, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("発音を聞く")
            }

            Spacer(Modifier.height(24.dp))
            // 覚えた / 苦手
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { mark(1) }) { Text("覚えた") }
                OutlinedButton(onClick = { mark(2) }) { Text("苦手") }
            }

            Spacer(Modifier.height(24.dp))
            // 前後移動
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(
                    onClick = { goTo(safeIndex - 1) },
                    enabled = safeIndex > 0,
                ) { Text("前へ") }
                Button(
                    onClick = { goTo(safeIndex + 1) },
                    enabled = safeIndex < words.size - 1,
                ) { Text("次へ") }
            }
        }
    }
}
