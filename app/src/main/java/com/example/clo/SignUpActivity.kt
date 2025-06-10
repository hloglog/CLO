package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = Firebase.auth
        db = Firebase.firestore

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordConfirm = findViewById<EditText>(R.id.etPasswordConfirm)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvSwitchToLogin = findViewById<TextView>(R.id.tvSwitchToLogin)

        btnSignUp.setOnClickListener {
            // 입력값 가져오기
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val passwordConfirm = etPasswordConfirm.text.toString().trim()

            // 입력값 검증
            when {
                email.isEmpty() -> {
                    etEmail.error = "이메일을 입력해주세요"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "올바른 이메일 형식이 아닙니다"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    etPassword.error = "비밀번호를 입력해주세요"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    etPassword.error = "비밀번호는 6자 이상이어야 합니다"
                    etPassword.requestFocus()
                    return@setOnClickListener
                }
                passwordConfirm.isEmpty() -> {
                    etPasswordConfirm.error = "비밀번호 확인을 입력해주세요"
                    etPasswordConfirm.requestFocus()
                    return@setOnClickListener
                }
                password != passwordConfirm -> {
                    etPasswordConfirm.error = "비밀번호가 일치하지 않습니다"
                    etPasswordConfirm.requestFocus()
                    return@setOnClickListener
                }
            }

            // 로딩 표시
            btnSignUp.isEnabled = false
            btnSignUp.text = "회원가입 중..."

            Log.d(TAG, "회원가입 시작 - 이메일: $email")

            // 회원가입 진행
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Firebase 회원가입 성공")
                        
                        // Firestore에 사용자 정보 저장
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            Log.d(TAG, "사용자 ID 생성됨: $userId")
                            
                            val username = email.split("@")[0]
                            val userData = hashMapOf(
                                "email" to email,
                                "name" to username,
                                "createdAt" to Date(),
                                "profileImageUrl" to null,
                                "followers" to emptyList<String>(),
                                "following" to 0
                            )
                            
                            Log.d(TAG, "사용자 데이터 저장 시도: $userData")
                            
                            try {
                                Log.d(TAG, "Firestore 저장 시작")
                                
                                // 타임아웃 설정 (10초)
                                val timeoutHandler = Handler(Looper.getMainLooper())
                                val timeoutRunnable = Runnable {
                                    Log.e(TAG, "Firestore 작업 타임아웃")
                                    runOnUiThread {
                                        Toast.makeText(this@SignUpActivity,
                                            "데이터 저장 시간이 초과되었습니다.",
                                            Toast.LENGTH_SHORT).show()
                                        btnSignUp.isEnabled = true
                                        btnSignUp.text = "회원가입"
                                        auth.currentUser?.delete()
                                    }
                                }
                                timeoutHandler.postDelayed(timeoutRunnable, 10000)

                                // Firestore에 사용자 데이터 저장
                                db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener {
                                        timeoutHandler.removeCallbacks(timeoutRunnable)
                                        Log.d(TAG, "사용자 데이터 저장 성공")
                                        
                                        // SharedPreferences에 로그인 상태 저장
                                        val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
                                        with (sharedPref.edit()) {
                                            putBoolean("is_logged_in", true)
                                            putString("user_id", userId)
                                            apply()
                                        }
                                        
                                        Log.d(TAG, "SharedPreferences 저장 완료")
                                        
                                        // UI 업데이트는 메인 스레드에서 실행
                                        runOnUiThread {
                                            try {
                                                Log.d(TAG, "화면 전환 시도")
                                                Toast.makeText(this@SignUpActivity, 
                                                    "회원가입이 완료되었습니다.", 
                                                    Toast.LENGTH_SHORT).show()
                                                
                                                // 홈 화면으로 이동
                                                val intent = Intent(this@SignUpActivity, 
                                                    MainActivity::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                                           Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                }
                                                Log.d(TAG, "Intent 생성 완료")
                                                startActivity(intent)
                                                Log.d(TAG, "startActivity 호출 완료")
                                                finish()
                                                Log.d(TAG, "finish 호출 완료")
                                            } catch (e: Exception) {
                                                Log.e(TAG, "화면 이동 중 오류 발생: ${e.message}", e)
                                                Toast.makeText(this@SignUpActivity,
                                                    "화면 이동 중 오류가 발생했습니다: ${e.message}",
                                                    Toast.LENGTH_SHORT).show()
                                                btnSignUp.isEnabled = true
                                                btnSignUp.text = "회원가입"
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        timeoutHandler.removeCallbacks(timeoutRunnable)
                                        Log.e(TAG, "사용자 데이터 저장 실패: ${e.message}", e)
                                        runOnUiThread {
                                            try {
                                                Toast.makeText(this@SignUpActivity, 
                                                    "사용자 정보 저장 실패: ${e.message}", 
                                                    Toast.LENGTH_SHORT).show()
                                                btnSignUp.isEnabled = true
                                                btnSignUp.text = "회원가입"
                                                auth.currentUser?.delete()
                                            } catch (e: Exception) {
                                                Log.e(TAG, "오류 처리 중 예외 발생", e)
                                            }
                                        }
                                    }
                            } catch (e: Exception) {
                                Log.e(TAG, "Firestore 작업 중 예외 발생: ${e.message}", e)
                                runOnUiThread {
                                    Toast.makeText(this@SignUpActivity, 
                                        "데이터베이스 오류: ${e.message}", 
                                        Toast.LENGTH_SHORT).show()
                                    btnSignUp.isEnabled = true
                                    btnSignUp.text = "회원가입"
                                    auth.currentUser?.delete()
                                }
                            }
                        } else {
                            Log.e(TAG, "사용자 ID가 null입니다")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(this@SignUpActivity, 
                                    "사용자 정보 생성 실패", 
                                    Toast.LENGTH_SHORT).show()
                                btnSignUp.isEnabled = true
                                btnSignUp.text = "회원가입"
                            }
                        }
                    } else {
                        Log.e(TAG, "Firebase 회원가입 실패", task.exception)
                        val errorMessage = when {
                            task.exception?.message?.contains("email address is already in use") == true ->
                                "이미 사용 중인 이메일입니다"
                            task.exception?.message?.contains("badly formatted") == true ->
                                "잘못된 이메일 형식입니다"
                            else -> "회원가입 실패: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        btnSignUp.isEnabled = true
                        btnSignUp.text = "회원가입"
                    }
                }
        }

        tvSwitchToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}