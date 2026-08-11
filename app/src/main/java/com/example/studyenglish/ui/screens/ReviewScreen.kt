package com.example.studyenglish.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.studyenglish.data.db.Word
import com.example.studyenglish.ui.rememberRepository

/** 苦手・お気に入りをまとめて復習する画面。対象は開始時に固定する。 */
@Composable
fun ReviewScreen(
    mode: String, // "weak" | "favorite"
    onBack: () -> Unit,
) {
    val repository = rememberRepository()
    var words by remember { mutableStateOf<List<Word>?>(null) }

    LaunchedEffect(mode) {
        words = if (mode == "favorite") repository.favoriteWordsOnce() else repository.weakWordsOnce()
    }

    val title = if (mode == "favorite") "お気に入りの復習" else "苦手の復習"
    val loaded = words
    if (loaded == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        StudyCards(title = title, words = loaded, onBack = onBack)
    }
}
