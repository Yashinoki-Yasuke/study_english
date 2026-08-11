package com.example.studyenglish

import android.app.Application
import com.example.studyenglish.data.StudyRepository
import com.example.studyenglish.data.db.AppDatabase

class StudyApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { StudyRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // 起動時にDBを初期化（必要なら語彙データを投入）しておく
        database
        // 広告SDKの初期化は、同意取得（UMP）後に MainActivity から行う。
        // 同意フォーム表示には Activity が必要なため。
    }
}
