package com.example.studyenglish.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** 発音リスニングの再生状態 */
data class PlaybackState(
    /** セッションが読み込まれているか（再生画面が有効か） */
    val active: Boolean = false,
    val isPlaying: Boolean = false,
    val lessonId: Long = -1,
    val lessonTitle: String = "",
    /** 現在の単語のインデックス（0始まり） */
    val index: Int = 0,
    val total: Int = 0,
    val currentEnglish: String = "",
    val currentJapanese: String = "",
    /** 現在読み上げているのが英語か（false=日本語） */
    val speakingEnglish: Boolean = true,
    /** リピート再生（レッスンを繰り返す） */
    val repeat: Boolean = false,
)

/**
 * サービスとUIの間で再生状態を共有する単純なバス。
 * サービスが更新し、UIが購読する。
 */
object PlaybackBus {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    fun update(newState: PlaybackState) {
        _state.value = newState
    }

    fun reset() {
        _state.value = PlaybackState()
    }
}
