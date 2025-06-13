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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query

class MyPageFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textUsername: TextView
    private lateinit var textFollowers: TextView
    private lateinit var imageProfile: ImageView
    
    // TODAY 섹션 뷰들
    private lateinit var todayTopCard: CardView
    private lateinit var todayBottomCard: CardView
    private lateinit var todayShoesCard: CardView
    private lateinit var todayAccessoriesCard: CardView
    private lateinit var todayTitleTextView: TextView
    
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
        
        // TODAY 섹션 뷰들 바인딩
        todayTopCard = view.findViewById(R.id.todayTopCard)
        todayBottomCard = view.findViewById(R.id.todayBottomCard)
        todayShoesCard = view.findViewById(R.id.todayShoesCard)
        todayAccessoriesCard = view.findViewById(R.id.todayAccessoriesCard)
        todayTitleTextView = view.findViewById(R.id.todayTitleTextView)

        // 현재 로그인된 사용자 정보 표시
        loadUserProfile()
        
        // TODAY 섹션 로드
        loadTodayOutfit()

        // 프로필 설정 버튼 클릭 리스너
        val buttonProfileSettings = view.findViewById<Button>(R.id.button_profile_settings)
        buttonProfileSettings.setOnClickListener {
            val intent = Intent(activity, ProfileSettingsActivity::class.java)
            startActivity(intent)
        }

        // CLOSET 버튼 클릭 리스너
        val buttonCloset = view.findViewById<Button>(R.id.button_closet)
        buttonCloset.setOnClickListener {
            findNavController().navigate(
                R.id.menu_closet,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.menu_mypage, false)
                    .build()
            )
        }

        // TODAY 섹션 클릭 리스너
        setupTodaySectionClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 포커스를 받을 때마다 프로필 정보와 TODAY 새로고침
        if (textUsername.text.isNullOrEmpty()) {
            loadUserProfile()
        }
        loadTodayOutfit()
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
                    val profileImageUrl = document.getString("profileImageUrl")
                    
                    textUsername.text = name ?: auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                    textFollowers.text = "팔로워 ${followers}명"
                    
                    // 프로필 이미지 로드
                    if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.default_profile_image)
                            .error(R.drawable.default_profile_image)
                            .into(imageProfile)
                    } else {
                        imageProfile.setImageResource(R.drawable.default_profile_image)
                    }
                } else {
                    Log.d(TAG, "사용자 문서가 존재하지 않음")
                    textUsername.text = auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                    textFollowers.text = "팔로워 0명"
                    imageProfile.setImageResource(R.drawable.default_profile_image)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "프로필 정보 로드 실패", e)
                Toast.makeText(context, 
                    "프로필 정보 로드 실패: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
                textUsername.text = auth.currentUser?.email?.split("@")?.get(0) ?: "사용자"
                textFollowers.text = "팔로워 0명"
                imageProfile.setImageResource(R.drawable.default_profile_image)
            }
    }

    private fun setupTodaySectionClickListeners() {
        // TODAY 전체 섹션 클릭 시 업로드 화면으로 이동
        val todayCard = view?.findViewById<CardView>(R.id.todayCard)
        todayCard?.setOnClickListener {
            val intent = Intent(activity, OutfitUploadActivity::class.java)
            startActivity(intent)
        }
        
        // 개별 카드들도 클릭 가능하도록 설정
        todayTopCard.setOnClickListener {
            val intent = Intent(activity, OutfitUploadActivity::class.java)
            startActivity(intent)
        }
        todayBottomCard.setOnClickListener {
            val intent = Intent(activity, OutfitUploadActivity::class.java)
            startActivity(intent)
        }
        todayShoesCard.setOnClickListener {
            val intent = Intent(activity, OutfitUploadActivity::class.java)
            startActivity(intent)
        }
        todayAccessoriesCard.setOnClickListener {
            val intent = Intent(activity, OutfitUploadActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadTodayOutfit() {
        val userId = auth.currentUser?.uid
        if (userId == null) return
        
        // 오늘 0시 ~ 내일 0시 Timestamp 구하기
        val now = java.util.Calendar.getInstance()
        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
        now.set(java.util.Calendar.MINUTE, 0)
        now.set(java.util.Calendar.SECOND, 0)
        now.set(java.util.Calendar.MILLISECOND, 0)
        val startTimestamp = com.google.firebase.Timestamp(now.time)
        now.add(java.util.Calendar.DATE, 1)
        val endTimestamp = com.google.firebase.Timestamp(now.time)
        
        Log.d(TAG, "TODAY 쿼리: userId=$userId, start=$startTimestamp, end=$endTimestamp")
        
        db.collection("today")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "TODAY 쿼리 결과: ${documents.size()}개 문서")
                
                // 클라이언트에서 오늘 날짜 필터링
                val todayDocuments = documents.filter { doc ->
                    val docTimestamp = doc.getTimestamp("timestamp")
                    docTimestamp != null && 
                    docTimestamp >= startTimestamp && 
                    docTimestamp < endTimestamp
                }
                
                if (todayDocuments.isNotEmpty()) {
                    val doc = todayDocuments.first()
                    Log.d(TAG, "TODAY 문서 ID: ${doc.id}")
                    val topImageUrl = doc.getString("topImageUrl") ?: ""
                    val bottomImageUrl = doc.getString("bottomImageUrl") ?: ""
                    val shoesImageUrl = doc.getString("shoesImageUrl") ?: ""
                    val accessoriesImageUrl = doc.getString("accessoriesImageUrl") ?: ""
                    
                    Log.d(TAG, "TODAY 이미지 URL들: top=$topImageUrl, bottom=$bottomImageUrl, shoes=$shoesImageUrl, accessories=$accessoriesImageUrl")
                    
                    // 각 카테고리별로 이미지 표시
                    updateTodayCard(todayTopCard, topImageUrl, "상의")
                    updateTodayCard(todayBottomCard, bottomImageUrl, "하의")
                    updateTodayCard(todayShoesCard, shoesImageUrl, "신발")
                    updateTodayCard(todayAccessoriesCard, accessoriesImageUrl, "기타")
                    
                    todayTitleTextView.text = "TODAY (수정하기)"
                } else {
                    // 오늘의 착장이 없으면 기본 상태로 표시
                    Log.d(TAG, "오늘의 착장이 없습니다")
                    todayTitleTextView.text = "TODAY (새로 만들기)"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "TODAY 착장 로드 실패", e)
                todayTitleTextView.text = "TODAY"
            }
    }
    
    private fun updateTodayCard(card: CardView, imageUrl: String, categoryName: String) {
        val imageView = when (card.id) {
            R.id.todayTopCard -> requireView().findViewById<ImageView>(R.id.today_top_image)
            R.id.todayBottomCard -> requireView().findViewById<ImageView>(R.id.today_bottom_image)
            R.id.todayShoesCard -> requireView().findViewById<ImageView>(R.id.today_shoes_image)
            R.id.todayAccessoriesCard -> requireView().findViewById<ImageView>(R.id.today_accessories_image)
            else -> null
        }
        if (imageView != null) {
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        }
    }
} 