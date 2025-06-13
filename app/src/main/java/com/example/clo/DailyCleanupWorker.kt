package com.example.clo

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class DailyCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db: FirebaseFirestore = Firebase.firestore
    private val TAG = "DailyCleanupWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "일일 정리 작업 시작")
            
            // 어제 날짜의 시작과 끝 시간 계산
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, -1) // 어제로 설정
            
            // 어제 0시
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val yesterdayStart = com.google.firebase.Timestamp(calendar.time)
            
            // 어제 23시 59분 59초
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val yesterdayEnd = com.google.firebase.Timestamp(calendar.time)
            
            Log.d(TAG, "어제 날짜 범위: $yesterdayStart ~ $yesterdayEnd")
            
            // 어제 날짜의 모든 TODAY 게시글 조회
            val querySnapshot = db.collection("today")
                .whereGreaterThanOrEqualTo("timestamp", yesterdayStart)
                .whereLessThanOrEqualTo("timestamp", yesterdayEnd)
                .get()
                .await()
            
            Log.d(TAG, "삭제할 게시글 수: ${querySnapshot.size()}")
            
            // 각 게시글 삭제
            val deletePromises = querySnapshot.documents.map { document ->
                db.collection("today").document(document.id).delete()
            }
            
            // 모든 삭제 작업 완료 대기
            deletePromises.forEach { it.await() }
            
            Log.d(TAG, "일일 정리 작업 완료: ${querySnapshot.size()}개 게시글 삭제됨")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "일일 정리 작업 실패", e)
            Result.failure()
        }
    }
} 