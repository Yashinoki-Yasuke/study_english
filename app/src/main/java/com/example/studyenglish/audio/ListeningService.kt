package com.example.studyenglish.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.studyenglish.MainActivity
import com.example.studyenglish.data.SettingsStore
import com.example.studyenglish.data.db.AppDatabase
import com.example.studyenglish.data.db.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 発音リスニングのバックグラウンド再生サービス。
 * TextToSpeech を使い「英語 → （間）→ 日本語 → 次の単語」を交互に再生する。
 * フォアグラウンドサービス + メディア通知でアプリを閉じても再生を継続する。
 */
class ListeningService : Service() {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var startWhenReady = false

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private var words: List<Word> = emptyList()
    private var sourceKey: String = ""
    private var lessonTitle: String = ""
    private var index = 0
    private var speakingEnglish = true
    private var isPlaying = false
    private var repeat = false

    // 設定（速度・間隔・シャッフル）
    private lateinit var settings: SettingsStore
    private var speechRate = 1.0f
    private var pauseEnJaMs = 700L
    private var pauseWordsMs = 500L

    // 発話ごとに一意のIDを振る（IDを使い回すと onDone が届かなくなるため）
    private var utteranceSeq = 0
    private var currentUtteranceId = ""
    // スキップ等で、現在の発話終了後に読み直したいときに立てる
    private var pendingRestart = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        settings = SettingsStore(this)
        applySettings()
        audioManager = getSystemService(AudioManager::class.java)
        mediaSession = MediaSessionCompat(this, "StudyEnglishListening").apply { isActive = true }
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                // メディア用途として再生（バックグラウンドでのミュート回避）
                tts.setAudioAttributes(audioAttributes)
                tts.setSpeechRate(speechRate)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        // 古い（既に置き換えられた）発話の遅延コールバックは無視する
                        handler.post {
                            if (utteranceId == currentUtteranceId) onUtteranceDone()
                        }
                    }
                })
                if (startWhenReady) {
                    startWhenReady = false
                    speakCurrent()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val key = intent.getStringExtra(EXTRA_SOURCE_KEY).orEmpty()
                val title = intent.getStringExtra(EXTRA_LESSON_TITLE).orEmpty()
                repeat = intent.getBooleanExtra(EXTRA_REPEAT, false)
                // 5秒以内に startForeground する必要があるため、先に通知を表示
                goForeground()
                loadAndStart(key, title)
            }
            ACTION_TOGGLE -> togglePlay()
            ACTION_NEXT -> skip(+1)
            ACTION_PREV -> skip(-1)
            ACTION_TOGGLE_REPEAT -> {
                repeat = !repeat
                publishState()
                updateNotification()
            }
            ACTION_APPLY_SETTINGS -> {
                // 再生中に速度・間隔を反映（シャッフルは次回開始時に適用）
                applySettings()
                if (ttsReady) tts.setSpeechRate(speechRate)
            }
            ACTION_STOP -> stopPlaybackAndService()
        }
        return START_NOT_STICKY
    }

    /** 設定（速度・間隔・シャッフル）を読み込む */
    private fun applySettings() {
        speechRate = settings.listeningSpeed
        pauseEnJaMs = settings.listeningPauseMs.toLong()
        pauseWordsMs = (pauseEnJaMs * 0.7).toLong().coerceAtLeast(200)
    }

    private fun loadAndStart(key: String, title: String) {
        sourceKey = key
        lessonTitle = title
        applySettings()
        if (ttsReady) tts.setSpeechRate(speechRate)
        ioScope.launch {
            val dao = AppDatabase.getInstance(applicationContext).wordDao()
            val id = key.substringAfter(":", "-1").toLongOrNull() ?: -1L
            val loaded = if (key.startsWith("course:")) {
                dao.getWordsByCourseOnce(id)
            } else {
                dao.getWordsByLessonOnce(id)
            }
            withContext(Dispatchers.Main) {
                words = if (settings.listeningShuffle) loaded.shuffled() else loaded
                index = 0
                speakingEnglish = true
                isPlaying = true
                pendingRestart = false
                if (words.isEmpty()) {
                    stopPlaybackAndService()
                    return@withContext
                }
                requestAudioFocus()
                if (ttsReady) speakCurrent() else startWhenReady = true
                publishState()
                updateNotification()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        // 既に取得済みなら再要求しない（再要求すると古いリスナーにLOSSが飛び再生が止まるため）
        if (focusRequest != null) return true
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // 他アプリに奪われたら一時停止（発話は中断しない）
                        if (isPlaying) {
                            isPlaying = false
                            handler.removeCallbacksAndMessages(null)
                            publishState()
                            updateNotification()
                        }
                    }
                }
            }
            .build()
        focusRequest = request
        val result = audioManager.requestAudioFocus(request)
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    /** 現在の位置・フェーズに応じて読み上げる */
    private fun speakCurrent() {
        if (!isPlaying || index !in words.indices) return
        val word = words[index]
        val id = "utt_${++utteranceSeq}"
        currentUtteranceId = id
        if (speakingEnglish) {
            tts.language = Locale.US
            tts.speak(word.english, TextToSpeech.QUEUE_FLUSH, null, id)
        } else {
            tts.language = Locale.JAPANESE
            tts.speak(word.japanese, TextToSpeech.QUEUE_FLUSH, null, id)
        }
        publishState()
        updateNotification()
    }

    private fun onUtteranceDone() {
        // スキップ等で再スタート予約がある場合は、その位置から読み直す
        if (pendingRestart) {
            pendingRestart = false
            if (isPlaying) speakCurrent()
            return
        }
        // フェーズ／位置を進める（一時停止中でも位置は進め、再開時に次の発話へ）
        if (speakingEnglish) {
            speakingEnglish = false
        } else {
            speakingEnglish = true
            index++
            if (index >= words.size) {
                if (repeat) {
                    index = 0 // リピートON: 先頭に戻って継続
                } else {
                    finishPlayback()
                    return
                }
            }
        }
        if (!isPlaying) {
            // 一時停止中: 位置だけ更新して待機（再開時にここから読む）
            publishState()
            updateNotification()
            return
        }
        // 英語の直後は英日間の間、日本語の直後は単語間の間を空けて次へ
        val delay = if (!speakingEnglish) pauseEnJaMs else pauseWordsMs
        handler.postDelayed({ if (isPlaying && !tts.isSpeaking()) speakCurrent() }, delay)
    }

    private fun togglePlay() {
        if (words.isEmpty()) return
        if (isPlaying) {
            // 一時停止: 発話は中断しない（中断すると次のonDoneが失われるため）。
            // 次のスケジュールのみ止め、再生中の語は最後まで鳴らす。
            isPlaying = false
            handler.removeCallbacksAndMessages(null)
        } else {
            isPlaying = true
            requestAudioFocus()
            // まだ前の語を発話中なら、その完了時のonDoneで自然に継続する
            if (!tts.isSpeaking()) speakCurrent()
        }
        publishState()
        updateNotification()
    }

    private fun skip(delta: Int) {
        if (words.isEmpty()) return
        handler.removeCallbacksAndMessages(null)
        index = (index + delta).coerceIn(0, words.size - 1)
        speakingEnglish = true
        if (isPlaying) {
            // 発話中なら中断せず、完了後にジャンプ先から読み直す
            if (tts.isSpeaking()) pendingRestart = true else speakCurrent()
        } else {
            publishState()
            updateNotification()
        }
    }

    /** 最後まで再生し終えた。停止状態にして先頭付近で待機。 */
    private fun finishPlayback() {
        isPlaying = false
        index = words.size - 1
        speakingEnglish = true
        publishState()
        updateNotification()
    }

    private fun stopPlaybackAndService() {
        isPlaying = false
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) tts.stop()
        abandonAudioFocus()
        PlaybackBus.reset()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publishState() {
        val word = words.getOrNull(index)
        PlaybackBus.update(
            PlaybackState(
                active = words.isNotEmpty(),
                isPlaying = isPlaying,
                sourceKey = sourceKey,
                lessonTitle = lessonTitle,
                index = index,
                total = words.size,
                currentEnglish = word?.english.orEmpty(),
                currentJapanese = word?.japanese.orEmpty(),
                speakingEnglish = speakingEnglish,
                repeat = repeat,
            )
        )
    }

    // ---- 通知 ----

    private fun goForeground() {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else 0,
        )
    }

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val word = words.getOrNull(index)
        val contentTitle = word?.english?.ifEmpty { lessonTitle } ?: lessonTitle.ifEmpty { "発音リスニング" }
        val contentText = buildString {
            if (word != null) append(word.japanese)
            if (words.isNotEmpty()) append("   (${index + 1}/${words.size})")
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "前へ", servicePendingIntent(ACTION_PREV))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "一時停止" else "再生",
                servicePendingIntent(ACTION_TOGGLE),
            )
            .addAction(android.R.drawable.ic_media_next, "次へ", servicePendingIntent(ACTION_NEXT))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", servicePendingIntent(ACTION_STOP))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
        return builder.build()
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, ListeningService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "発音リスニング",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "発音の再生コントロール" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (::mediaSession.isInitialized) mediaSession.release()
        abandonAudioFocus()
        PlaybackBus.reset()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "listening_channel"
        private const val NOTIF_ID = 1001


        const val ACTION_START = "com.example.studyenglish.action.START"
        const val ACTION_TOGGLE = "com.example.studyenglish.action.TOGGLE"
        const val ACTION_NEXT = "com.example.studyenglish.action.NEXT"
        const val ACTION_PREV = "com.example.studyenglish.action.PREV"
        const val ACTION_TOGGLE_REPEAT = "com.example.studyenglish.action.TOGGLE_REPEAT"
        const val ACTION_APPLY_SETTINGS = "com.example.studyenglish.action.APPLY_SETTINGS"
        const val ACTION_STOP = "com.example.studyenglish.action.STOP"
        const val EXTRA_SOURCE_KEY = "sourceKey"
        const val EXTRA_LESSON_TITLE = "lessonTitle"
        const val EXTRA_REPEAT = "repeat"

        fun lessonKey(lessonId: Long) = "lesson:$lessonId"
        fun courseKey(courseId: Long) = "course:$courseId"

        /** sourceKey は "lesson:ID" または "course:ID" */
        fun start(context: Context, sourceKey: String, title: String, repeat: Boolean = false) {
            val intent = Intent(context, ListeningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SOURCE_KEY, sourceKey)
                putExtra(EXTRA_LESSON_TITLE, title)
                putExtra(EXTRA_REPEAT, repeat)
            }
            context.startForegroundService(intent)
        }

        fun sendAction(context: Context, action: String) {
            context.startService(Intent(context, ListeningService::class.java).setAction(action))
        }
    }
}
