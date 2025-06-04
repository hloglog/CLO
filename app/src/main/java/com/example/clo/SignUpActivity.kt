package com.example.clo

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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.*

class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val TAG = "SignUpActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = Firebase.auth
        database = FirebaseDatabase.getInstance().apply {
            // 데이터베이스 URL이 설정되어 있지 않다면 설정
            if (reference.root.toString() == "https://null.firebaseio.com") {
                Log.e(TAG, "Firebase Database URL이 설정되지 않았습니다!")
                Toast.makeText(this@SignUpActivity, "데이터베이스 연결 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                return@apply
            }
        }
        Log.d(TAG, "Firebase Database URL: ${database.reference.root.toString()}")

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

            // 회원가입 진행
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Firebase 회원가입 성공")
                        Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        
                        // 바로 자동 로그인 시도
                        Log.d(TAG, "자동 로그인 시도 시작 - 이메일: $email")
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { loginTask ->
                                if (loginTask.isSuccessful) {
                                    Log.d(TAG, "자동 로그인 성공 - 사용자: ${auth.currentUser?.uid}")
                                    Toast.makeText(this, "자동 로그인 성공", Toast.LENGTH_SHORT).show()
                                    
                                    // 홈 화면으로 이동
                                    val intent = Intent(this, HomeActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Log.e(TAG, "자동 로그인 실패: ${loginTask.exception?.message}")
                                    Toast.makeText(this, "자동 로그인 실패: ${loginTask.exception?.message}", 
                                        Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    startActivity(intent)
                                    finish()
                                }
                            }
                    } else {
                        Log.e(TAG, "Firebase 회원가입 실패", task.exception)
                        val errorMessage = when {
                            task.exception?.message?.contains("email address is already in use") == true ->
                                "이미 사용 중인 이메일입니다"
                            task.exception?.message?.contains("badly formatted") == true ->
                                "잘못된 이메일 형식입니다"
                            else -> "회원가입에 실패했습니다: ${task.exception?.message}"
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