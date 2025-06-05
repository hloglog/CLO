package com.example.clo

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CLOApplication : Application() {
    private val TAG = "CLOApplication"

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Firebase 초기화
            FirebaseApp.initializeApp(this)
            
            // Firestore 초기화
            val db = Firebase.firestore
            db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            
            Log.d(TAG, "Firebase 초기화 성공")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 실패", e)
        }
    }
} 
