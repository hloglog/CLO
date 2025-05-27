package com.example.clo

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class CLOApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase Database 지속성 설정
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
} 
