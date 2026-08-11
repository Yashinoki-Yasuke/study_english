package com.example.studyenglish.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.studyenglish.MainActivity
import com.example.studyenglish.data.SettingsStore

/**
 * リマインド時刻に発火して通知を表示する。
 * 端末再起動(BOOT_COMPLETED)時は保存済み設定からアラームを再設定する。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val settings = SettingsStore(context)
                if (settings.reminderEnabled) {
                    ReminderScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
                }
            }
            else -> showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("英語学習の時間です")
            .setContentText("今日の学習を始めましょう！")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, notification)
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "学習リマインド",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "毎日の学習リマインド通知" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIF_ID = 2002
    }
}
