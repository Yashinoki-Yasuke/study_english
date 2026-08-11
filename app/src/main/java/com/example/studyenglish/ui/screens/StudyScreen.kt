package com.example.studyenglish.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import com.example.studyenglish.ui.ads.BannerAd
import com.example.studyenglish.ui.ads.InterstitialAdManager
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyenglish.audio.WordSpeaker
import com.example.studyenglish.data.db.Word
import com.example.studyenglish.ui.rememberRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    lessonId: Long,
    lessonTitle: String,
    onBack: () -> Unit,
) {
    val repository = rememberRepository()
    val wordsFlow = remember(lessonId) { repository.words(lessonId) }
    val words by wordsFlow.collectAsState(initial = emptyList())
    StudyCards(title = lessonTitle.ifEmpty { "学習" }, words = words, onBack = onBack)
}

/**
 * 学習カードの共通UI。レッスン学習・苦手復習・お気に入り復習で共有する。
 * カードをタップで意味の表示切替、発音再生、覚えた/苦手の記録、★お気に入り、前後移動。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCards(
    title: String,
    words: List<Word>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = rememberRepository()
    val scope = rememberCoroutineScope()

    val speaker = remember { WordSpeaker(context) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }

    // 学習終了（画面を離れる）時に区切りの全画面広告を表示
    LaunchedEffect(Unit) { InterstitialAdManager.preload(context) }
    fun leave() { InterstitialAdManager.maybeShow(context) { onBack() } }
    BackHandler { leave() }

    var index by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        if (words.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { Text("対象の単語がありません") }
            return@Scaffold
        }

        val safeIndex = index.coerceIn(0, words.size - 1)
        val word = words[safeIndex]
        val progress by remember(word.id) { repository.progress(word.id) }
            .collectAsState(initial = null)
        val isFavorite = progress?.isFavorite == true

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${safeIndex + 1} / ${words.size}", style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = {
                    scope.launch { repository.setFavorite(word.id, !isFavorite) }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (isFavorite) "お気に入り解除" else "お気に入りに追加",
                        tint = if (isFavorite) Color(0xFFF9A825) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAnswer = !showAnswer },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
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
                        textAlign = TextAlign.Center,
                    )
                    word.phonetic?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    if (showAnswer) {
                        Text(word.japanese, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        word.example?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    } else {
                        TextButton(onClick = { showAnswer = true }) { Text("タップして意味を表示") }
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { mark(1) }) { Text("覚えた") }
                OutlinedButton(onClick = { mark(2) }) { Text("苦手") }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Button(onClick = { goTo(safeIndex - 1) }, enabled = safeIndex > 0) { Text("前へ") }
                Button(onClick = { goTo(safeIndex + 1) }, enabled = safeIndex < words.size - 1) { Text("次へ") }
            }
        }
    }
}
