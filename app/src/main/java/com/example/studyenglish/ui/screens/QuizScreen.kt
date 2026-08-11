package com.example.studyenglish.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyenglish.data.db.Word
import com.example.studyenglish.ui.rememberRepository
import kotlinx.coroutines.launch

private data class QuizQuestion(
    val word: Word,
    val options: List<String>,
    val correctIndex: Int,
)

private fun buildQuestions(words: List<Word>): List<QuizQuestion> {
    if (words.isEmpty()) return emptyList()
    val allJapanese = words.map { it.japanese }.distinct()
    return words.shuffled().map { word ->
        val distractors = allJapanese
            .filter { it != word.japanese }
            .shuffled()
            .take(3)
        val options = (distractors + word.japanese).shuffled()
        QuizQuestion(word, options, options.indexOf(word.japanese))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    lessonId: Long,
    lessonTitle: String,
    onBack: () -> Unit,
) {
    val repository = rememberRepository()
    val scope = rememberCoroutineScope()
    val wordsFlow = remember(lessonId) { repository.words(lessonId) }
    val words by wordsFlow.collectAsState(initial = emptyList())

    // 出題は words 確定時に一度だけ生成（再生成トリガー用のキー）
    var quizKey by remember { mutableIntStateOf(0) }
    val questions = remember(words, quizKey) { buildQuestions(words) }

    var qIndex by remember(quizKey) { mutableIntStateOf(0) }
    var selected by remember(quizKey) { mutableStateOf<Int?>(null) }
    var correctCount by remember(quizKey) { mutableIntStateOf(0) }
    var finished by remember(quizKey) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle.ifEmpty { "クイズ" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        }
    ) { innerPadding ->
        if (questions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text("出題できる単語がありません") }
            return@Scaffold
        }

        if (finished) {
            ResultView(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                correct = correctCount,
                total = questions.size,
                onRetry = { quizKey++ },
                onBack = onBack,
            )
            return@Scaffold
        }

        val question = questions[qIndex]

        fun choose(optionIndex: Int) {
            if (selected != null) return
            selected = optionIndex
            val isCorrect = optionIndex == question.correctIndex
            if (isCorrect) correctCount++
            // 正誤を学習状態へ反映（正解=覚えた, 不正解=苦手）
            scope.launch {
                repository.setWordStatus(question.word.id, if (isCorrect) 1 else 2)
            }
        }

        fun next() {
            if (qIndex < questions.size - 1) {
                qIndex++
                selected = null
            } else {
                finished = true
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                progress = { (qIndex + 1f) / questions.size },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("${qIndex + 1} / ${questions.size}", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("この単語の意味は？", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = question.word.english,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            question.options.forEachIndexed { i, option ->
                val containerColor = when {
                    selected == null -> MaterialTheme.colorScheme.surfaceVariant
                    i == question.correctIndex -> Color(0xFF2E7D32) // 正解=緑
                    i == selected -> Color(0xFFC62828)              // 選んだ不正解=赤
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = when {
                    selected == null -> MaterialTheme.colorScheme.onSurface
                    i == question.correctIndex || i == selected -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Button(
                    onClick = { choose(i) }, // 回答後は choose() 側で無視される
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                    ),
                ) {
                    Text(option, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            if (selected != null) {
                val isCorrect = selected == question.correctIndex
                Text(
                    text = if (isCorrect) "正解！" else "不正解… 正解: ${question.options[question.correctIndex]}",
                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { next() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (qIndex < questions.size - 1) "次の問題へ" else "結果を見る")
                }
            }
        }
    }
}

@Composable
private fun ResultView(
    modifier: Modifier,
    correct: Int,
    total: Int,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("結果", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$correct / $total 正解",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        val rate = if (total > 0) correct * 100 / total else 0
        Text(
            text = when {
                rate == 100 -> "満点！素晴らしい🎉"
                rate >= 70 -> "よくできました！"
                rate >= 40 -> "その調子！"
                else -> "復習してまた挑戦しよう"
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("もう一度挑戦")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("戻る")
        }
    }
}
