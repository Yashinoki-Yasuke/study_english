package com.example.studyenglish.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice

/** 英語TTS音声の列挙・表示名づけ */
object TtsVoices {

    /** 利用可能な英語音声（未インストールを除く）を国・名前順で返す */
    fun englishVoices(tts: TextToSpeech): List<Voice> =
        (tts.voices ?: emptySet())
            .filter {
                it.locale.language == "en" &&
                    it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
            }
            .sortedWith(compareBy({ it.locale.country }, { it.name }))

    /** 国コードから日本語のアクセント名 */
    fun accentName(country: String): String = when (country) {
        "US" -> "アメリカ英語"
        "GB" -> "イギリス英語"
        "AU" -> "オーストラリア英語"
        "IN" -> "インド英語"
        "CA" -> "カナダ英語"
        "IE" -> "アイルランド英語"
        "ZA" -> "南アフリカ英語"
        "NG" -> "ナイジェリア英語"
        else -> "英語"
    }

    /**
     * 一覧全体から、各音声に「アメリカ英語 1（オンライン）」のような表示名を割り当てる。
     * 同じアクセント内で連番を付ける。
     */
    fun labels(voices: List<Voice>): Map<String, String> {
        val counts = HashMap<String, Int>()
        val result = LinkedHashMap<String, String>()
        for (v in voices) {
            val accent = accentName(v.locale.country)
            val n = (counts[accent] ?: 0) + 1
            counts[accent] = n
            val online = if (v.isNetworkConnectionRequired) "（オンライン）" else ""
            result[v.name] = "$accent $n$online"
        }
        return result
    }
}
