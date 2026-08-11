package com.example.studyenglish.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 学習画面で単語を1回だけ読み上げる軽量なTTSラッパー。
 * 連続再生はしないので ListeningService とは別に、画面の生存期間だけ保持する。
 */
class WordSpeaker(context: Context) {

    private var ready = false
    private var pending: (() -> Unit)? = null
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
                pending?.invoke()
                pending = null
            }
        }
    }

    /** 英語を読み上げる */
    fun speakEnglish(text: String) {
        val action: () -> Unit = {
            tts.language = Locale.US
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
