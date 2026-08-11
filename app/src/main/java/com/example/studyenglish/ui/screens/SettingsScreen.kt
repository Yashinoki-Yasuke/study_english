package com.example.studyenglish.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studyenglish.data.SettingsStore
import com.example.studyenglish.notification.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var enabled by remember { mutableStateOf(store.reminderEnabled) }
    var hour by remember { mutableIntStateOf(store.reminderHour) }
    var minute by remember { mutableIntStateOf(store.reminderMinute) }

    fun applySchedule() {
        if (enabled) {
            ReminderScheduler.schedule(context, hour, minute)
        } else {
            ReminderScheduler.cancel(context)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> applySchedule() }

    fun enableWithPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            applySchedule()
        }
    }

    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, h, m ->
                hour = h
                minute = m
                store.reminderHour = h
                store.reminderMinute = m
                if (enabled) applySchedule()
            },
            hour, minute, true,
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
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
                .padding(innerPadding),
        ) {
            // リマインドON/OFF
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("毎日のリマインド通知", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "決めた時刻に学習をお知らせします",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        store.reminderEnabled = it
                        if (it) enableWithPermission() else applySchedule()
                    },
                )
            }
            HorizontalDivider()
            // 通知時刻
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { openTimePicker() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "通知時刻",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!enabled) {
                        Text(
                            "リマインドをONにすると設定できます",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            // 英語の音声（TTS Voice）選択
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVoiceSettings() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("英語の音声", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "アメリカ英語・イギリス英語などから選べます",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text("▶", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()

            // データ出典（ライセンス表記）
            Column(modifier = Modifier.padding(16.dp)) {
                Text("データ出典", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "見出し語: NGSL / BSL / TSL / NAWL (Browne, Culligan & Phillips) — CC BY-SA 4.0\n" +
                        "日本語訳: JMdict (© EDRDG) — CC BY-SA 4.0\n" +
                        "フレーズ: Tatoeba Project — CC BY 2.0 (France)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider()
        }
    }
}
