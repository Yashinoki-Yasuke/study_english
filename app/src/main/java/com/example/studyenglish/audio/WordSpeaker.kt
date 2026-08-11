package com.example.studyenglish.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.example.studyenglish.data.SettingsStore
import java.util.Locale

/**
 * 学習画面で単語を1回だけ読み上げる軽量なTTSラッパー。
 * 連続再生はしないので ListeningService とは別に、画面の生存期間だけ保持する。
 */
class WordSpeaker(context: Context) {

    private val settings = SettingsStore(context)
    private var ready = false
    private var pending: (() -> Unit)? = null
    private var englishVoice: Voice? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                resolveVoice()
                pending?.invoke()
                pending = null
            }
        }
    }

    private fun resolveVoice() {
        val name = settings.ttsVoice
        englishVoice = if (name.isBlank()) null else tts.voices?.firstOrNull { it.name == name }
    }

    /** 英語を読み上げる（設定された音声を使用） */
    fun speakEnglish(text: String) {
        val action: () -> Unit = {
            resolveVoice()
            val v = englishVoice
            if (v != null) tts.voice = v else tts.language = Locale.US
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word_speak")
        }
        if (ready) action() else pending = action
    }

    fun shutdown() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
