package com.example.studyenglish

import android.app.Application
import com.example.studyenglish.data.StudyRepository
import com.example.studyenglish.data.db.AppDatabase

class StudyApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { StudyRepository(database) }
}
