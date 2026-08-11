package com.example.studyenglish.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyenglish.audio.ListeningService
import com.example.studyenglish.audio.PlaybackBus
import com.example.studyenglish.data.SettingsStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen(
    lessonId: Long,
    lessonTitle: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by PlaybackBus.state.collectAsState()

    // このレッスンのセッションが有効か
    val activeForThisLesson = state.active && state.lessonId == lessonId

    // 再生前に選んでおくリピート設定（再生中はサービスの状態を優先）
    var repeatDesired by remember { mutableStateOf(false) }
    val repeatOn = if (activeForThisLesson) state.repeat else repeatDesired

    // 速度・間隔・シャッフル設定（SettingsStoreに保存）
    val store = remember { SettingsStore(context) }
    var speed by remember { mutableFloatStateOf(store.listeningSpeed) }
    var pauseMs by remember { mutableIntStateOf(store.listeningPauseMs) }
    var shuffle by remember { mutableStateOf(store.listeningShuffle) }

    fun applyToService() {
        if (activeForThisLesson) {
            ListeningService.sendAction(context, ListeningService.ACTION_APPLY_SETTINGS)
        }
    }
    fun setSpeed(v: Float) { speed = v; store.listeningSpeed = v; applyToService() }
    fun setPause(v: Int) { pauseMs = v; store.listeningPauseMs = v; applyToService() }
    fun setShuffle(v: Boolean) { shuffle = v; store.listeningShuffle = v } // シャッフルは次回開始時に反映

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 許可の可否にかかわらず再生を開始（通知が出ないだけで再生は可能）
        ListeningService.start(context, lessonId, lessonTitle, repeatDesired)
    }

    fun startListening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            ListeningService.start(context, lessonId, lessonTitle, repeatDesired)
        }
    }

    fun toggleRepeat() {
        if (activeForThisLesson) {
            ListeningService.sendAction(context, ListeningService.ACTION_TOGGLE_REPEAT)
        } else {
            repeatDesired = !repeatDesired
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle.ifEmpty { "発音リスニング" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "英語→日本語を交互に再生します。\nアプリを閉じても再生は続きます。",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // 現在の単語カード
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                    if (activeForThisLesson) {
                        Text(
                            text = "${state.index + 1} / ${state.total}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.currentEnglish,
                            fontSize = 32.sp,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.currentJapanese,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when {
                                !state.isPlaying -> "⏸ 一時停止中"
                                state.speakingEnglish -> "🔊 英語を再生中"
                                else -> "🔊 日本語を再生中"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    } else {
                        Text(
                            text = "「再生開始」を押してください",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // リピート／シャッフル
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = repeatOn,
                    onClick = { toggleRepeat() },
                    label = { Text("リピート") },
                    leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null) },
                )
                FilterChip(
                    selected = shuffle,
                    onClick = { setShuffle(!shuffle) },
                    label = { Text("シャッフル") },
                    leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null) },
                )
            }

            Spacer(Modifier.height(12.dp))
            // 再生速度
            Text("再生速度", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x", 1.5f to "1.5x").forEach { (v, label) ->
                    FilterChip(selected = speed == v, onClick = { setSpeed(v) }, label = { Text(label) })
                }
            }

            Spacer(Modifier.height(12.dp))
            // 英語→日本語の間隔
            Text("間隔（英語→日本語）", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(400 to "短い", 700 to "普通", 1200 to "長い").forEach { (ms, label) ->
                    FilterChip(selected = pauseMs == ms, onClick = { setPause(ms) }, label = { Text(label) })
                }
            }

            Spacer(Modifier.height(16.dp))

            if (activeForThisLesson) {
                // 再生コントロール
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IconButton(onClick = { ListeningService.sendAction(context, ListeningService.ACTION_PREV) }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "前へ", modifier = Modifier.size(40.dp))
                    }
                    FilledIconButton(
                        onClick = { ListeningService.sendAction(context, ListeningService.ACTION_TOGGLE) },
                        modifier = Modifier.size(72.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying)
                                androidx.compose.material.icons.Icons.Filled.Pause
                            else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "一時停止" else "再生",
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = { ListeningService.sendAction(context, ListeningService.ACTION_NEXT) }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "次へ", modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { ListeningService.sendAction(context, ListeningService.ACTION_STOP) }) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("停止")
                }
            } else {
                Button(onClick = { startListening() }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("再生開始")
                }
            }
        }
    }
}
