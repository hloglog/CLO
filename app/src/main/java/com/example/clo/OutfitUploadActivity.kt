package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import com.bumptech.glide.Glide

class OutfitUploadActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    // 카테고리 버튼들
    private lateinit var topButton: MaterialButton
    private lateinit var bottomButton: MaterialButton
    private lateinit var shoesButton: MaterialButton
    private lateinit var accessoriesButton: MaterialButton
    
    // RecyclerView
    private lateinit var clothesRecyclerView: RecyclerView
    private lateinit var clothesAdapter: ClothesAdapter
    
    // TODAY 섹션
    private lateinit var todayTopCard: MaterialCardView
    private lateinit var todayBottomCard: MaterialCardView
    private lateinit var todayShoesCard: MaterialCardView
    private lateinit var todayAccessoriesCard: MaterialCardView
    
    // 선택된 옷들
    private var selectedTop: ClosetActivity.ClothingItem? = null
    private var selectedBottom: ClosetActivity.ClothingItem? = null
    private var selectedShoes: ClosetActivity.ClothingItem? = null
    private var selectedAccessories: ClosetActivity.ClothingItem? = null
    
    // 현재 게시글 ID (수정 시 사용)
    private var currentTodayId: String? = null
    
    companion object {
        private const val TAG = "OutfitUploadActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfit_upload)
        
        auth = Firebase.auth
        db = Firebase.firestore
        
        initializeViews()
        setupBottomNavigation()
        setupCategoryButtons()
        setupTodayCards()
        
        // 기존 TODAY 게시글 로드
        loadExistingTodayOutfit()
    }
    
    private fun initializeViews() {
        // 카테고리 버튼들
        topButton = findViewById(R.id.topButton)
        bottomButton = findViewById(R.id.bottomButton)
        shoesButton = findViewById(R.id.shoesButton)
        accessoriesButton = findViewById(R.id.accessoriesButton)
        
        // RecyclerView
        clothesRecyclerView = findViewById(R.id.clothesRecyclerView)
        clothesAdapter = ClothesAdapter { item ->
            showAddToTodayDialog(item)
        }
        clothesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        clothesRecyclerView.adapter = clothesAdapter
        
        // TODAY 카드들
        todayTopCard = findViewById(R.id.todayTopCard)
        todayBottomCard = findViewById(R.id.todayBottomCard)
        todayShoesCard = findViewById(R.id.todayShoesCard)
        todayAccessoriesCard = findViewById(R.id.todayAccessoriesCard)
        
        // 게시글 올리기 버튼
        val postButton = findViewById<MaterialButton>(R.id.postButton)
        postButton.setOnClickListener {
            postTodayOutfit()
        }
        
        // 버튼 텍스트는 loadExistingTodayOutfit에서 설정됨
    }
    
    private fun updatePostButtonText() {
        val postButton = findViewById<MaterialButton>(R.id.postButton)
        if (currentTodayId != null) {
            postButton.text = "착장 수정하기"
        } else {
            postButton.text = "게시글 올리기"
        }
    }
    
    private fun setupCategoryButtons() {
        // 초기 상태: 상의 선택
        selectCategory("상의")
        
        topButton.setOnClickListener { selectCategory("상의") }
        bottomButton.setOnClickListener { selectCategory("하의") }
        shoesButton.setOnClickListener { selectCategory("신발") }
        accessoriesButton.setOnClickListener { selectCategory("기타") }
    }
    
    private fun selectCategory(category: String) {
        // 모든 버튼을 기본 스타일로 초기화
        resetAllButtons()
        
        when (category) {
            "상의" -> {
                topButton.setBackgroundColor(resources.getColor(R.color.black, theme))
                topButton.setTextColor(resources.getColor(R.color.white, theme))
            }
            "하의" -> {
                bottomButton.setBackgroundColor(resources.getColor(R.color.black, theme))
                bottomButton.setTextColor(resources.getColor(R.color.white, theme))
            }
            "신발" -> {
                shoesButton.setBackgroundColor(resources.getColor(R.color.black, theme))
                shoesButton.setTextColor(resources.getColor(R.color.white, theme))
            }
            "기타" -> {
                accessoriesButton.setBackgroundColor(resources.getColor(R.color.black, theme))
                accessoriesButton.setTextColor(resources.getColor(R.color.white, theme))
            }
        }
        
        // 선택된 카테고리의 옷을 로드
        loadClothes(category)
    }
    
    private fun resetAllButtons() {
        val buttons = listOf(topButton, bottomButton, shoesButton, accessoriesButton)
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(R.color.white, theme))
            button.setTextColor(resources.getColor(R.color.black, theme))
        }
    }
    
    private fun loadClothes(category: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            clothesAdapter.submitList(emptyList())
            return
        }
        
        db.collection("clothes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { documents ->
                val clothesList = documents.mapNotNull { doc ->
                    val imageUrl = doc.getString("imageUrl")
                    if (imageUrl != null) {
                        ClosetActivity.ClothingItem(
                            id = doc.id,
                            imageUrl = imageUrl,
                            category = category
                        )
                    } else null
                }
                clothesAdapter.submitList(clothesList)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading clothes", e)
                Toast.makeText(this, "옷장을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                clothesAdapter.submitList(emptyList())
            }
    }
    
    private fun showAddToTodayDialog(item: ClosetActivity.ClothingItem) {
        val categoryName = when (item.category) {
            "상의" -> "상의"
            "하의" -> "하의"
            "신발" -> "신발"
            "기타" -> "기타"
            else -> item.category
        }
        
        AlertDialog.Builder(this)
            .setTitle("TODAY에 추가")
            .setMessage("이 ${categoryName}을 TODAY에 올리겠습니까?")
            .setPositiveButton("추가") { _, _ ->
                addToToday(item)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun addToToday(item: ClosetActivity.ClothingItem) {
        when (item.category) {
            "상의" -> selectedTop = item
            "하의" -> selectedBottom = item
            "신발" -> selectedShoes = item
            "기타" -> selectedAccessories = item
        }
        
        updateTodayDisplay()
        Toast.makeText(this, "${item.category}이 TODAY에 추가되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateTodayDisplay() {
        // TODAY 카드들 업데이트
        updateTodayCard(todayTopCard, selectedTop, "상의")
        updateTodayCard(todayBottomCard, selectedBottom, "하의")
        updateTodayCard(todayShoesCard, selectedShoes, "신발")
        updateTodayCard(todayAccessoriesCard, selectedAccessories, "기타")
    }
    
    private fun updateTodayCard(card: MaterialCardView, item: ClosetActivity.ClothingItem?, categoryName: String) {
        if (item != null) {
            // 선택된 옷이 있으면 이미지 표시
            card.setCardBackgroundColor(resources.getColor(R.color.white, theme))
            
            // 기존 TextView 제거하고 ImageView 추가
            card.removeAllViews()
            
            val imageView = android.widget.ImageView(this)
            imageView.layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            
            // Glide로 이미지 로드
            Glide.with(this)
                .load(item.imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView)
            
            card.addView(imageView)
        } else {
            // 선택된 옷이 없으면 기본 상태로 복원
            card.setCardBackgroundColor(resources.getColor(R.color.gray_light, theme))
            
            // 기존 뷰 제거하고 TextView 추가
            card.removeAllViews()
            
            val textView = android.widget.TextView(this)
            textView.layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textView.text = categoryName
            textView.setTextColor(resources.getColor(R.color.gray_dark, theme))
            textView.textSize = 16f
            textView.gravity = android.view.Gravity.CENTER
            
            // TextView를 카드 중앙에 배치
            val layoutParams = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = android.view.Gravity.CENTER
            textView.layoutParams = layoutParams
            
            card.addView(textView)
        }
    }
    
    private fun setupTodayCards() {
        // TODAY 카드들에 클릭 리스너 추가 (선택 해제 기능)
        todayTopCard.setOnClickListener {
            selectedTop?.let {
                selectedTop = null
                updateTodayDisplay()
                Toast.makeText(this, "상의가 제거되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        
        todayBottomCard.setOnClickListener {
            selectedBottom?.let {
                selectedBottom = null
                updateTodayDisplay()
                Toast.makeText(this, "하의가 제거되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        
        todayShoesCard.setOnClickListener {
            selectedShoes?.let {
                selectedShoes = null
                updateTodayDisplay()
                Toast.makeText(this, "신발이 제거되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        
        todayAccessoriesCard.setOnClickListener {
            selectedAccessories?.let {
                selectedAccessories = null
                updateTodayDisplay()
                Toast.makeText(this, "기타가 제거되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun postTodayOutfit() {
        val selectedItems = listOfNotNull(selectedTop, selectedBottom, selectedShoes, selectedAccessories)
        
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "최소 하나의 옷을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Firebase에 저장할 데이터
        val todayOutfit = hashMapOf(
            "userId" to userId,
            "topImageUrl" to (selectedTop?.imageUrl ?: ""),
            "bottomImageUrl" to (selectedBottom?.imageUrl ?: ""),
            "shoesImageUrl" to (selectedShoes?.imageUrl ?: ""),
            "accessoriesImageUrl" to (selectedAccessories?.imageUrl ?: ""),
            "timestamp" to Timestamp.now()
        )
        
        if (currentTodayId != null) {
            // 기존 게시글 업데이트
            db.collection("today").document(currentTodayId!!)
                .update(todayOutfit as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "오늘의 착장이 수정되었습니다!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating today outfit", e)
                    Toast.makeText(this, "수정 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 새 게시글 생성
            db.collection("today")
                .add(todayOutfit)
                .addOnSuccessListener { documentReference ->
                    currentTodayId = documentReference.id
                    Toast.makeText(this, "오늘의 착장이 게시되었습니다!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error posting today outfit", e)
                    Toast.makeText(this, "게시 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.menu_mypage

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_search -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("fragment", "search")
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("fragment", "home")
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.menu_mypage -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("fragment", "profile")
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadExistingTodayOutfit() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
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
                    currentTodayId = doc.id
                    Log.d(TAG, "기존 TODAY 문서 ID: ${doc.id}")
                    
                    // 기존 데이터 로드
                    val topImageUrl = doc.getString("topImageUrl") ?: ""
                    val bottomImageUrl = doc.getString("bottomImageUrl") ?: ""
                    val shoesImageUrl = doc.getString("shoesImageUrl") ?: ""
                    val accessoriesImageUrl = doc.getString("accessoriesImageUrl") ?: ""
                    
                    Log.d(TAG, "기존 TODAY 이미지 URL들: top=$topImageUrl, bottom=$bottomImageUrl, shoes=$shoesImageUrl, accessories=$accessoriesImageUrl")
                    
                    // 각 카테고리별로 옷 정보 로드
                    loadClothingItemFromUrl(topImageUrl, "상의") { item ->
                        selectedTop = item
                        updateTodayDisplay()
                    }
                    loadClothingItemFromUrl(bottomImageUrl, "하의") { item ->
                        selectedBottom = item
                        updateTodayDisplay()
                    }
                    loadClothingItemFromUrl(shoesImageUrl, "신발") { item ->
                        selectedShoes = item
                        updateTodayDisplay()
                    }
                    loadClothingItemFromUrl(accessoriesImageUrl, "기타") { item ->
                        selectedAccessories = item
                        updateTodayDisplay()
                    }
                    
                    // 버튼 텍스트 업데이트
                    updatePostButtonText()
                    
                    Toast.makeText(this, "오늘의 착장을 불러왔습니다. 수정해보세요!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "오늘의 착장이 없습니다")
                    // 버튼 텍스트 업데이트
                    updatePostButtonText()
                    Toast.makeText(this, "새로운 오늘의 착장을 만들어보세요!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "기존 TODAY 게시글 로드 실패", e)
                Toast.makeText(this, "기존 착장을 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadClothingItemFromUrl(imageUrl: String, category: String, onLoaded: (ClosetActivity.ClothingItem?) -> Unit) {
        if (imageUrl.isEmpty()) {
            onLoaded(null)
            return
        }
        
        // clothes 컬렉션에서 해당 이미지 URL을 가진 문서 찾기
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onLoaded(null)
            return
        }
        
        db.collection("clothes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("imageUrl", imageUrl)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val item = ClosetActivity.ClothingItem(
                        id = doc.id,
                        imageUrl = imageUrl,
                        category = category
                    )
                    onLoaded(item)
                } else {
                    onLoaded(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "옷 정보 로드 실패", e)
                onLoaded(null)
            }
    }
} 