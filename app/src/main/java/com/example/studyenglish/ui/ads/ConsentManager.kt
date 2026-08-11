package com.example.studyenglish.ui.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UMP（User Messaging Platform）による同意管理。
 * GDPR等の対象地域では広告表示前にユーザー同意の取得が必須のため、
 * 起動時に同意情報を更新し、必要なら同意フォームを表示する。
 *
 * 同意取得後（または不要な地域で）広告リクエスト可能になったら MobileAds を初期化する。
 * MobileAds の初期化は一度きり（AtomicBooleanでガード）。
 *
 * ※ 実際に同意フォームを出すには AdMob 管理画面で
 *   「プライバシーとメッセージ」→ GDPR同意メッセージを公開しておく必要がある。
 *   未設定でも canRequestAds() は true を返し、広告初期化は正常に行われる。
 */
object ConsentManager {

    private val adsInitialized = AtomicBoolean(false)

    /**
     * 同意情報を更新し、必要なら同意フォームを表示する。
     * 完了後（同意有無にかかわらず）広告リクエスト可能なら MobileAds を初期化する。
     */
    fun gatherConsentAndInitAds(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 同意情報の更新に成功。必要なら同意フォームを表示。
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // フォーム完了（またはエラー）。要求可能なら初期化。
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAds(activity)
                    }
                }
            },
            {
                // 同意情報の更新に失敗（ネットワーク等）。
                // 直近のキャッシュで要求可能なら初期化を試みる。
                if (consentInformation.canRequestAds()) {
                    initializeMobileAds(activity)
                }
            },
        )

        // 既に同意取得済み等で最初から要求可能な場合は即初期化。
        if (consentInformation.canRequestAds()) {
            initializeMobileAds(activity)
        }
    }

    /** 広告SDKの初期化（重い処理なのでバックグラウンドで、かつ一度きり）。 */
    private fun initializeMobileAds(context: Context) {
        if (adsInitialized.getAndSet(true)) return
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(appContext)
        }
    }
}
