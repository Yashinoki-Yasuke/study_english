package com.example.studyenglish.data

import android.content.Context

/** リマインド設定などの小さな設定を保存する（SharedPreferences） */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_HOUR, 20)
        set(value) = prefs.edit().putInt(KEY_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_MINUTE, value).apply()

    // ---- 発音リスニング設定 ----
    /** 再生速度（0.5〜2.0） */
    var listeningSpeed: Float
        get() = prefs.getFloat(KEY_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEED, value).apply()

    /** 英語→日本語の間（ミリ秒）。単語間はこれより短くする */
    var listeningPauseMs: Int
        get() = prefs.getInt(KEY_PAUSE, 700)
        set(value) = prefs.edit().putInt(KEY_PAUSE, value).apply()

    /** シャッフル再生 */
    var listeningShuffle: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHUFFLE, value).apply()

    companion object {
        private const val KEY_ENABLED = "reminder_enabled"
        private const val KEY_HOUR = "reminder_hour"
        private const val KEY_MINUTE = "reminder_minute"
        private const val KEY_SPEED = "listening_speed"
        private const val KEY_PAUSE = "listening_pause"
        private const val KEY_SHUFFLE = "listening_shuffle"
    }
}
