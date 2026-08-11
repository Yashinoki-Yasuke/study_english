package com.example.studyenglish.ui.screens

import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import com.example.studyenglish.ui.ads.BannerAd
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studyenglish.audio.TtsVoices
import com.example.studyenglish.data.SettingsStore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var voices by remember { mutableStateOf<List<Voice>>(emptyList()) }
    var labels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selected by remember { mutableStateOf(store.ttsVoice) }
    val ttsRef = remember { arrayOfNulls<TextToSpeech>(1) }

    DisposableEffect(Unit) {
        val tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsRef[0]?.let { t ->
                    t.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    val list = TtsVoices.englishVoices(t)
                    voices = list
                    labels = TtsVoices.labels(list)
                }
            }
        }
        ttsRef[0] = tts
        onDispose { tts.stop(); tts.shutdown() }
    }

    fun preview(voice: Voice?) {
        val t = ttsRef[0] ?: return
        if (voice != null) t.voice = voice else t.language = Locale.US
        t.speak("Hello. This is a sample.", TextToSpeech.QUEUE_FLUSH, null, "preview")
    }

    fun select(name: String, voice: Voice?) {
        selected = name
        store.ttsVoice = name
        preview(voice)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("英語の音声") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        if (voices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { Text("音声を読み込んでいます…") }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            item {
                Text(
                    "選ぶと試聴します。オフラインで使うには端末にインストール済みの音声を選んでください。",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp),
                )
                // 端末の標準
                VoiceRow(
                    label = "端末の標準",
                    selected = selected.isBlank(),
                    onClick = { select("", null) },
                    onPreview = { preview(null) },
                )
                HorizontalDivider()
            }
            items(voices) { voice ->
                VoiceRow(
                    label = labels[voice.name] ?: voice.name,
                    selected = selected == voice.name,
                    onClick = { select(voice.name, voice) },
                    onPreview = { preview(voice) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun VoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onPreview) {
            Icon(Icons.Filled.VolumeUp, contentDescription = "試聴")
        }
    }
}
