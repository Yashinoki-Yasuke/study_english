package com.example.studyenglish

import android.app.Application
import com.example.studyenglish.data.StudyRepository
import com.example.studyenglish.data.db.AppDatabase
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { StudyRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // 起動時にDBを初期化（必要なら語彙データを投入）しておく
        database
        // 広告SDKの初期化は重いのでバックグラウンドスレッドで行う（ANR回避）
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@StudyApp)
        }
    }
}
