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
            // Firebase가 이미 초기화되었는지 확인
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Firebase 초기화
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase 초기화 성공")
            } else {
                Log.d(TAG, "Firebase가 이미 초기화되어 있음")
            }
            
            // Firestore 초기화 (try-catch로 감싸서 오류 방지)
            try {
                val db = Firebase.firestore
                // setPersistenceEnabled는 deprecated이므로 제거
                Log.d(TAG, "Firestore 초기화 완료")
            } catch (e: Exception) {
                Log.e(TAG, "Firestore 초기화 실패", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 실패", e)
        }
    }
} 
