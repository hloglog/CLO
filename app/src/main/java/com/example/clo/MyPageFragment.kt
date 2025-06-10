package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyPageFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textUsername: TextView
    private lateinit var textFollowers: TextView
    private lateinit var imageProfile: ImageView
    private val TAG = "MyPageFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        // (액티비티 단에서 처리되거나, 여기서는 프래그먼트 전환 로직에 따라 변경될 수 있음)
        // 현재는 MyPageActivity의 로직을 그대로 옮겨옴
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
            return
        }

        // 뷰 바인딩
        textUsername = view.findViewById(R.id.text_username)
        textFollowers = view.findViewById(R.id.text_followers)
        imageProfile = view.findViewById(R.id.image_profile)

        // 현재 로그인된 사용자 정보 표시
        loadUserProfile()

        // 프로필 설정 버튼 클릭 리스너
        val buttonProfileSettings = view.findViewById<Button>(R.id.button_profile_settings)
        buttonProfileSettings.setOnClickListener {
            val intent = Intent(activity, ProfileSettingsActivity::class.java)
            startActivity(intent)
        }

        // CLOSET 버튼 클릭 리스너
        val buttonCloset = view.findViewById<Button>(R.id.button_closet)
        buttonCloset.setOnClickListener {
            val intent = Intent(activity, ClosetActivity::class.java)
            startActivity(intent)
        }

        // TODO: TODAY 섹션 착장 목록 표시 (RecyclerView 등)
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 포커스를 받을 때마다 프로필 정보 새로고침
        loadUserProfile()
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = activity?.getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref?.getBoolean("is_logged_in", false) ?: false
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        // Firestore에서 사용자 정보 로드
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val followers = (document.get("followers") as? List<String>)?.size ?: 0
                    
                    textUsername.text = name ?: auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                    textFollowers.text = "팔로워 ${followers}명"
                    
                    // TODO: 프로필 이미지 로드 (Glide 등 사용)
                } else {
                    Log.d(TAG, "사용자 문서가 존재하지 않음")
                    textUsername.text = auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                    textFollowers.text = "팔로워 0명"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 정보 로드 실패", e)
                Toast.makeText(context, 
                    "프로필 정보 로드 실패: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                textUsername.text = auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                textFollowers.text = "팔로워 0명"
            }
    }
} 