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
    private var lessonId: Long = -1
    private var lessonTitle: String = ""
    private var index = 0
    private var speakingEnglish = true
    private var isPlaying = false
    private var repeat = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        audioManager = getSystemService(AudioManager::class.java)
        mediaSession = MediaSessionCompat(this, "StudyEnglishListening").apply { isActive = true }
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                // メディア用途として再生（バックグラウンドでのミュート回避）
                tts.setAudioAttributes(audioAttributes)
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        handler.post { onUtteranceDone() }
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
                val newLessonId = intent.getLongExtra(EXTRA_LESSON_ID, -1)
                val title = intent.getStringExtra(EXTRA_LESSON_TITLE).orEmpty()
                repeat = intent.getBooleanExtra(EXTRA_REPEAT, false)
                // 5秒以内に startForeground する必要があるため、先に通知を表示
                goForeground()
                loadAndStart(newLessonId, title)
            }
            ACTION_TOGGLE -> togglePlay()
            ACTION_NEXT -> skip(+1)
            ACTION_PREV -> skip(-1)
            ACTION_TOGGLE_REPEAT -> {
                repeat = !repeat
                publishState()
                updateNotification()
            }
            ACTION_STOP -> stopPlaybackAndService()
        }
        return START_NOT_STICKY
    }

    private fun loadAndStart(newLessonId: Long, title: String) {
        lessonId = newLessonId
        lessonTitle = title
        ioScope.launch {
            val loaded = AppDatabase.getInstance(applicationContext)
                .wordDao().getWordsByLessonOnce(newLessonId)
            withContext(Dispatchers.Main) {
                words = loaded
                index = 0
                speakingEnglish = true
                isPlaying = true
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
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // 他アプリに奪われたら一時停止
                        if (isPlaying) {
                            isPlaying = false
                            handler.removeCallbacksAndMessages(null)
                            tts.stop()
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
        if (speakingEnglish) {
            tts.language = Locale.US
            tts.speak(word.english, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } else {
            tts.language = Locale.JAPANESE
            tts.speak(word.japanese, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        }
        publishState()
        updateNotification()
    }

    private fun onUtteranceDone() {
        if (!isPlaying) return
        if (speakingEnglish) {
            // 英語の後、少し間を置いて日本語へ
            speakingEnglish = false
            handler.postDelayed({ if (isPlaying) speakCurrent() }, PAUSE_EN_JA_MS)
        } else {
            // 日本語の後、次の単語へ
            speakingEnglish = true
            index++
            if (index >= words.size) {
                if (repeat) {
                    // リピートON: 先頭に戻って継続
                    index = 0
                } else {
                    finishPlayback()
                    return
                }
            }
            handler.postDelayed({ if (isPlaying) speakCurrent() }, PAUSE_WORDS_MS)
        }
    }

    private fun togglePlay() {
        if (words.isEmpty()) return
        if (isPlaying) {
            isPlaying = false
            handler.removeCallbacksAndMessages(null)
            tts.stop()
        } else {
            isPlaying = true
            requestAudioFocus()
            speakCurrent()
        }
        publishState()
        updateNotification()
    }

    private fun skip(delta: Int) {
        if (words.isEmpty()) return
        handler.removeCallbacksAndMessages(null)
        tts.stop()
        index = (index + delta).coerceIn(0, words.size - 1)
        speakingEnglish = true
        if (isPlaying) {
            speakCurrent()
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
                lessonId = lessonId,
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
        private const val UTTERANCE_ID = "study_english_utt"

        private const val PAUSE_EN_JA_MS = 700L
        private const val PAUSE_WORDS_MS = 500L

        const val ACTION_START = "com.example.studyenglish.action.START"
        const val ACTION_TOGGLE = "com.example.studyenglish.action.TOGGLE"
        const val ACTION_NEXT = "com.example.studyenglish.action.NEXT"
        const val ACTION_PREV = "com.example.studyenglish.action.PREV"
        const val ACTION_TOGGLE_REPEAT = "com.example.studyenglish.action.TOGGLE_REPEAT"
        const val ACTION_STOP = "com.example.studyenglish.action.STOP"
        const val EXTRA_LESSON_ID = "lessonId"
        const val EXTRA_LESSON_TITLE = "lessonTitle"
        const val EXTRA_REPEAT = "repeat"

        fun start(context: Context, lessonId: Long, lessonTitle: String, repeat: Boolean = false) {
            val intent = Intent(context, ListeningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LESSON_ID, lessonId)
                putExtra(EXTRA_LESSON_TITLE, lessonTitle)
                putExtra(EXTRA_REPEAT, repeat)
            }
            context.startForegroundService(intent)
        }

        fun sendAction(context: Context, action: String) {
            context.startService(Intent(context, ListeningService::class.java).setAction(action))
        }
    }
}
