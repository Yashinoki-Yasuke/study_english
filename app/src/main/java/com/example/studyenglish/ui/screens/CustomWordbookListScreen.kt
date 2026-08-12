package com.example.studyenglish.ui.screens

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.studyenglish.ui.ads.BannerAd
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.studyenglish.data.db.Course
import com.example.studyenglish.ui.rememberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWordbookListScreen(
    onBack: () -> Unit,
    onOpenWordbook: (lessonId: Long, title: String) -> Unit,
) {
    val repository = rememberRepository()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val courses by repository.customCourses().collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var importTargetLessonId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val lessonId = importTargetLessonId
        importTargetLessonId = null
        if (uri != null && lessonId != null) {
            scope.launch {
                val lines = withContext(Dispatchers.IO) { readCsvLines(context, uri) }
                val result = repository.importWordsFromCsv(lessonId, lines)
                val msg = if (result.skipped > 0) {
                    "${result.added}件追加しました（${result.skipped}件スキップ）"
                } else {
                    "${result.added}件追加しました"
                }
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val templateSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) { writeCsvTemplate(context, uri) }
                snackbarHostState.showSnackbar("CSVテンプレートを保存しました")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("オリジナル単語帳") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { templateSaver.launch("wordbook_template.csv") }) {
                        Icon(Icons.Filled.Download, contentDescription = "CSVテンプレートを保存")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "新規作成")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { BannerAd() },
    ) { innerPadding ->
        if (courses.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("まだオリジナル単語帳がありません", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "右下の＋ボタンから作成できます。CSVファイル（1列目:英語, 2列目:日本語, " +
                        "3列目:発音記号(省略可), 4列目:例文(省略可)）を読み込んで単語を追加できます。",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { templateSaver.launch("wordbook_template.csv") }) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("CSVテンプレートを保存")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                items(courses, key = { it.id }) { course ->
                    WordbookRow(
                        course = course,
                        onOpen = onOpenWordbook,
                        onImportCsv = { lessonId ->
                            importTargetLessonId = lessonId
                            csvPicker.launch(arrayOf("text/*", "*/*"))
                        },
                        onDelete = { scope.launch { repository.deleteWordbook(course.id) } },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWordbookDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                scope.launch { repository.createWordbook(name) }
            },
        )
    }
}

@Composable
private fun WordbookRow(
    course: Course,
    onOpen: (lessonId: Long, title: String) -> Unit,
    onImportCsv: (lessonId: Long) -> Unit,
    onDelete: () -> Unit,
) {
    val repository = rememberRepository()
    var lessonId by remember(course.id) { mutableStateOf<Long?>(null) }
    val wordCount by remember(course.id) { repository.customWordCount(course.id) }
        .collectAsState(initial = 0)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(course.id) {
        lessonId = repository.wordbookLessonId(course.id)
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(course.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "削除")
                }
            }
            Text("$wordCount 語", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { lessonId?.let { onOpen(it, course.name) } },
                    enabled = lessonId != null,
                ) { Text("開く") }
                OutlinedButton(
                    onClick = { lessonId?.let { onImportCsv(it) } },
                    enabled = lessonId != null,
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(Modifier.size(4.dp))
                    Text("CSVを追加")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("削除しますか？") },
            text = { Text("「${course.name}」を削除すると、登録した単語もすべて削除されます。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun CreateWordbookDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しい単語帳") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("単語帳の名前") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("作成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

/** CSVファイルを読み込み、行ごとのリストにする（UTF-8のBOMは除去） */
private fun readCsvLines(context: Context, uri: Uri): List<String> {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = input.readBytes()
        var text = String(bytes, Charsets.UTF_8)
        if (text.isNotEmpty() && text[0].code == 0xFEFF) text = text.substring(1)
        text.lines()
    } ?: emptyList()
}

/** インポート用CSVのフォーマット見本（記入例つき） */
private const val CSV_TEMPLATE_CONTENT =
    "english,japanese,phonetic,example\n" +
        "apple,りんご,/ˈæpl/,I eat an apple every day.\n" +
        "run,走る,/rʌn/,I run every morning.\n"

/** テンプレートCSVを書き出す（Excel等での文字化けを避けるためBOM付きUTF-8で保存） */
private fun writeCsvTemplate(context: Context, uri: Uri) {
    context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())) // UTF-8 BOM
        out.write(CSV_TEMPLATE_CONTENT.toByteArray(Charsets.UTF_8))
    }
}
