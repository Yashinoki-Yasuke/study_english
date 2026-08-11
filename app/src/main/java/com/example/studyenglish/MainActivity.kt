package com.example.studyenglish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.studyenglish.ui.StudyNavHost
import com.example.studyenglish.ui.ads.ConsentManager
import com.example.studyenglish.ui.theme.StudyEnglishTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // UMPで同意を取得し、要求可能になったら広告SDKを初期化する
        ConsentManager.gatherConsentAndInitAds(this)
        setContent {
            StudyEnglishTheme {
                StudyNavHost()
            }
        }
    }
}
