package com.example.clo

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

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
                Firebase.firestore
                Log.d(TAG, "Firestore 초기화 완료")
            } catch (e: Exception) {
                Log.e(TAG, "Firestore 초기화 실패", e)
            }
            
            // WorkManager 초기화 및 일일 정리 작업 스케줄링
            setupDailyCleanupWork()
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase 초기화 실패", e)
        }
    }
    
    private fun setupDailyCleanupWork() {
        try {
            // 네트워크 연결이 있을 때만 실행되도록 제약 조건 설정
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // 매일 자정에 실행되는 주기적 작업 생성
            val dailyCleanupWorkRequest = PeriodicWorkRequestBuilder<DailyCleanupWorker>(
                1, TimeUnit.DAYS // 24시간마다 실행
            )
                .setConstraints(constraints)
                .build()
            
            // WorkManager에 작업 등록 (기존 작업이 있으면 업데이트)
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_cleanup_work",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyCleanupWorkRequest
            )
            
            Log.d(TAG, "일일 정리 작업 스케줄링 완료")
            
        } catch (e: Exception) {
            Log.e(TAG, "일일 정리 작업 스케줄링 실패", e)
        }
    }
} 
