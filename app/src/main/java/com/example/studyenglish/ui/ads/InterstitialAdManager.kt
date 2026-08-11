package com.example.studyenglish.ui.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * 全画面広告（インタースティシャル）の管理。
 * クイズ結果・学習終了などの「区切り」でのみ表示し、頻度制限で連続表示を防ぐ。
 *
 * ※ 広告ユニットIDは現在 Google公式の「テスト用」。リリース前に本番IDへ差し替えること。
 */
object InterstitialAdManager {

    private const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val MIN_INTERVAL_MS = 60_000L // 直前の表示から60秒は出さない

    private var ad: InterstitialAd? = null
    private var loading = false
    private var lastShownAt = 0L

    /** 事前読み込み（画面表示時などに呼ぶ） */
    fun preload(context: Context) {
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            context.applicationContext,
            TEST_INTERSTITIAL_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                }
            },
        )
    }

    /**
     * 区切りで全画面広告を表示する。広告が無い/頻度制限中/Activityが取れない場合は
     * 何もせず onDone() を呼ぶ。表示した場合は閉じたあとに onDone() を呼ぶ。
     */
    fun maybeShow(context: Context, onDone: () -> Unit) {
        val activity = context.findActivity()
        val current = ad
        val now = System.currentTimeMillis()
        if (activity == null || current == null || now - lastShownAt < MIN_INTERVAL_MS) {
            preload(context) // 次回のために読み込んでおく
            onDone()
            return
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                lastShownAt = System.currentTimeMillis()
                preload(context)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                preload(context)
                onDone()
            }
        }
        ad = null
        current.show(activity)
    }
}

/** Context から Activity を取り出す */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
