package com.example.studyenglish.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.studyenglish.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * バナー広告。
 * 広告ユニットIDは build.gradle.kts で debug=テスト用 / release=本番 を自動切り替え。
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    // 画面ごとに新しいAdViewを生成→表示のたびに広告が読み込まれる。
    // 画面を離れるとき(onRelease)に破棄してリークを防ぐ。
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
