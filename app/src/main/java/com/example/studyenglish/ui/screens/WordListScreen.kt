package com.example.studyenglish.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studyenglish.data.db.Word
import com.example.studyenglish.ui.rememberRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    lessonId: Long,
    lessonTitle: String,
    onBack: () -> Unit,
    onStudy: () -> Unit,
    onListen: () -> Unit,
) {
    val repository = rememberRepository()
    val wordsFlow = remember(lessonId) { repository.words(lessonId) }
    val words by wordsFlow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle.ifEmpty { "単語一覧" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            item {
                Button(
                    onClick = onStudy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Icon(Icons.Filled.School, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("カードで学習する")
                }
                Button(
                    onClick = onListen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Icon(Icons.Filled.Headset, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("このレッスンを聞く（発音リスニング）")
                }
            }
            items(words) { word ->
                WordRow(word)
            }
        }
    }
}

@Composable
private fun WordRow(word: Word) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(word.english, style = MaterialTheme.typography.titleMedium)
            Text(word.japanese, style = MaterialTheme.typography.bodyLarge)
            val meta = buildString {
                append(if (word.type == "idiom") "熟語" else "単語")
                word.phonetic?.let { append("  ").append(it) }
            }
            Text(meta, style = MaterialTheme.typography.bodySmall)
        }
    }
}
