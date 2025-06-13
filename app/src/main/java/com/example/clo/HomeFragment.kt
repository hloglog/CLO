package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.Query
import com.example.clo.AdapterItem
import com.example.clo.TodayOutfit

class HomeFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val TAG = "HomeFragment"

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
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
            return
        }

        usersRecyclerView = view.findViewById(R.id.recycler_view_outfits)
        usersRecyclerView.layoutManager = LinearLayoutManager(context)

        val adapterItems = mutableListOf<AdapterItem>()
        adapterItems.add(AdapterItem.Header)

        userAdapter = UserAdapter(adapterItems)
        usersRecyclerView.adapter = userAdapter

        loadTodayOutfitsFromFirebase()
    }

    private fun loadTodayOutfitsFromFirebase() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return
        
        // 오늘 0시 ~ 내일 0시 Timestamp 구하기
        val now = java.util.Calendar.getInstance()
        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
        now.set(java.util.Calendar.MINUTE, 0)
        now.set(java.util.Calendar.SECOND, 0)
        now.set(java.util.Calendar.MILLISECOND, 0)
        val startTimestamp = com.google.firebase.Timestamp(now.time)
        now.add(java.util.Calendar.DATE, 1)
        val endTimestamp = com.google.firebase.Timestamp(now.time)
        
        db.collection("today")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val outfitItems = mutableListOf<AdapterItem>()
                
                // 현재 사용자의 오늘 착장 확인
                val currentUserTodayOutfit = documents.filter { doc ->
                    val docUserId = doc.getString("userId")
                    val docTimestamp = doc.getTimestamp("timestamp")
                    docUserId == currentUserId && 
                    docTimestamp != null && 
                    docTimestamp >= startTimestamp && 
                    docTimestamp < endTimestamp
                }
                
                // 현재 사용자가 오늘 착장을 업로드하지 않았다면 헤더 추가
                if (currentUserTodayOutfit.isEmpty()) {
                    outfitItems.add(AdapterItem.Header)
                    Log.d(TAG, "현재 사용자가 오늘 착장을 업로드하지 않음 - 헤더 표시")
                } else {
                    // 착장을 업로드했다면 여백 아이템 추가
                    outfitItems.add(AdapterItem.Spacer)
                    Log.d(TAG, "현재 사용자가 이미 오늘 착장을 업로드함 - 여백 추가")
                }
                
                // 게시글들을 정렬: 현재 사용자 게시글을 맨 위로, 나머지는 시간순
                val currentUserOutfits = mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>()
                val otherUserOutfits = mutableListOf<com.google.firebase.firestore.QueryDocumentSnapshot>()
                
                // 현재 사용자와 다른 사용자의 게시글을 분리
                for (document in documents) {
                    val docUserId = document.getString("userId")
                    if (docUserId == currentUserId) {
                        currentUserOutfits.add(document)
                    } else {
                        otherUserOutfits.add(document)
                    }
                }
                
                // 현재 사용자 게시글을 시간순으로 정렬 (최신순)
                currentUserOutfits.sortByDescending { doc ->
                    doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now()
                }
                
                // 다른 사용자 게시글을 시간순으로 정렬 (최신순)
                otherUserOutfits.sortByDescending { doc ->
                    doc.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now()
                }
                
                // 현재 사용자 게시글을 먼저, 그 다음에 다른 사용자 게시글을 합치기
                val sortedDocuments = currentUserOutfits + otherUserOutfits
                
                for (document in sortedDocuments) {
                    val todayOutfit = TodayOutfit(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        username = "", // 나중에 사용자 정보에서 채워짐
                        topImageUrl = document.getString("topImageUrl") ?: "",
                        bottomImageUrl = document.getString("bottomImageUrl") ?: "",
                        shoesImageUrl = document.getString("shoesImageUrl") ?: "",
                        accessoriesImageUrl = document.getString("accessoriesImageUrl") ?: "",
                        outfitShotUrl = document.getString("outfitShotUrl"),
                        timestamp = document.getTimestamp("timestamp") ?: com.google.firebase.Timestamp.now(),
                        likeCount = document.getLong("likeCount")?.toInt() ?: 0,
                        likedBy = document.get("likedBy") as? List<String> ?: emptyList()
                    )
                    
                    // 사용자 정보도 함께 가져오기
                    loadUserInfoAndAddOutfit(todayOutfit, outfitItems)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "오늘의 착장 데이터 로드 실패", e)
                Toast.makeText(context, "오늘의 착장 데이터 로드 실패", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadUserInfoAndAddOutfit(todayOutfit: TodayOutfit, outfitItems: MutableList<AdapterItem>) {
        db.collection("users").document(todayOutfit.userId).get()
            .addOnSuccessListener { userDoc ->
                val username = userDoc.getString("name") ?: "사용자"
                val profileImageUrl = userDoc.getString("profileImageUrl")
                val updatedOutfit = todayOutfit.copy(
                    username = username,
                    profileImageUrl = profileImageUrl
                )
                outfitItems.add(AdapterItem.TodayOutfitItem(updatedOutfit))
                userAdapter.updateItems(outfitItems)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "사용자 정보 로드 실패", e)
                // 사용자 정보가 없어도 착장은 표시
                outfitItems.add(AdapterItem.TodayOutfitItem(todayOutfit))
                userAdapter.updateItems(outfitItems)
            }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = activity?.getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref?.getBoolean("is_logged_in", false) ?: false
    }

    // UserAdapter 클래스를 TodayOutfitItem도 처리하도록 수정
    private class UserAdapter(private var items: List<AdapterItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_SPACER = 1
        private val VIEW_TYPE_TODAY_OUTFIT = 2

        fun updateItems(newItems: List<AdapterItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is AdapterItem.Header -> VIEW_TYPE_HEADER
                is AdapterItem.Spacer -> VIEW_TYPE_SPACER
                is AdapterItem.TodayOutfitItem -> VIEW_TYPE_TODAY_OUTFIT
                else -> VIEW_TYPE_TODAY_OUTFIT
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_upload_header, parent, false)
                    val holder = HeaderViewHolder(view)
                    holder.itemView.findViewById<CardView>(R.id.card_upload_outfit).setOnClickListener {
                        val context = holder.itemView.context
                        context.startActivity(Intent(context, OutfitUploadActivity::class.java))
                    }
                    holder
                }
                VIEW_TYPE_SPACER -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_spacer, parent, false)
                    SpacerViewHolder(view)
                }
                VIEW_TYPE_TODAY_OUTFIT -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_outfit, parent, false)
                    TodayOutfitViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> holder.bind()
                is SpacerViewHolder -> holder.bind()
                is TodayOutfitViewHolder -> {
                    val outfitItem = items[position] as? AdapterItem.TodayOutfitItem
                    outfitItem?.let { holder.bind(it.outfit) }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                // 클릭 리스너는 onCreateViewHolder에서 설정됨
            }
        }

        class SpacerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                // 여백 아이템은 별도 처리 없음
            }
        }

        class TodayOutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageProfile: ImageView = itemView.findViewById(R.id.image_profile)
            private val textUsername: TextView = itemView.findViewById(R.id.text_username)
            private val textLikeCount: TextView = itemView.findViewById(R.id.text_like_count)
            private val iconLike: ImageView = itemView.findViewById(R.id.icon_like)

            // XML에 추가된 이미지뷰 참조
            private val topImageView: ImageView = itemView.findViewById(R.id.top_image)
            private val bottomImageView: ImageView = itemView.findViewById(R.id.bottom_image)
            private val shoesImageView: ImageView = itemView.findViewById(R.id.shoes_image)
            private val accessoriesImageView: ImageView = itemView.findViewById(R.id.accessories_image)
            private val outfitShotImageView: ImageView = itemView.findViewById(R.id.outfit_shot_image)
            
            // 현재 바인딩된 outfit 객체를 저장
            private var currentOutfit: TodayOutfit? = null

            fun bind(outfit: TodayOutfit) {
                currentOutfit = outfit
                textUsername.text = outfit.username
                
                // 프로필 이미지 로드
                if (outfit.profileImageUrl != null && outfit.profileImageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(outfit.profileImageUrl)
                        .placeholder(R.drawable.default_profile_image)
                        .error(R.drawable.default_profile_image)
                        .into(imageProfile)
                } else {
                    imageProfile.setImageResource(R.drawable.default_profile_image)
                }
                
                // 착용샷 이미지 로드
                if (outfit.outfitShotUrl != null && outfit.outfitShotUrl.isNotEmpty()) {
                    outfitShotImageView.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(outfit.outfitShotUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(outfitShotImageView)
                } else {
                    outfitShotImageView.visibility = View.GONE
                }
                
                // 좋아요 수 표시
                textLikeCount.text = "${outfit.likeCount}"
                
                // 현재 사용자가 좋아요를 눌렀는지 확인
                val currentUserId = Firebase.auth.currentUser?.uid
                val isLiked = currentUserId != null && outfit.likedBy.contains(currentUserId)
                
                // 좋아요 아이콘 상태 설정
                if (isLiked) {
                    iconLike.setImageResource(R.drawable.ic_favorite_filled_black_24dp)
                } else {
                    iconLike.setImageResource(R.drawable.ic_favorite_border_black_24dp)
                }
                
                // 이전 클릭 리스너 제거 후 새로운 리스너 설정
                iconLike.setOnClickListener(null)
                iconLike.setOnClickListener {
                    Log.d("HomeFragment", "좋아요 버튼 클릭됨: outfitId=${outfit.id}")
                    currentUserId?.let { userId ->
                        toggleLike(userId)
                    }
                }

                // 각 이미지뷰에 이미지 로드
                loadImageIntoView(topImageView, outfit.topImageUrl)
                loadImageIntoView(bottomImageView, outfit.bottomImageUrl)
                loadImageIntoView(shoesImageView, outfit.shoesImageUrl)
                loadImageIntoView(accessoriesImageView, outfit.accessoriesImageUrl)
            }
            
            private fun toggleLike(userId: String) {
                // 중복 클릭 방지
                iconLike.isClickable = false
                
                val currentOutfit = currentOutfit ?: return
                val db = Firebase.firestore
                val outfitRef = db.collection("today").document(currentOutfit.id)
                
                val isCurrentlyLiked = currentOutfit.likedBy.contains(userId)
                Log.d("HomeFragment", "좋아요 토글: outfitId=${currentOutfit.id}, userId=$userId, isCurrentlyLiked=$isCurrentlyLiked, currentLikedBy=${currentOutfit.likedBy}")
                
                val newLikedBy = if (isCurrentlyLiked) {
                    currentOutfit.likedBy.toMutableList().apply { remove(userId) }
                } else {
                    currentOutfit.likedBy.toMutableList().apply { add(userId) }
                }
                val newLikeCount = if (isCurrentlyLiked) currentOutfit.likeCount - 1 else currentOutfit.likeCount + 1
                
                Log.d("HomeFragment", "새로운 상태: newLikeCount=$newLikeCount, newLikedBy=$newLikedBy")
                
                // Firestore 업데이트
                outfitRef.update(
                    mapOf(
                        "likeCount" to newLikeCount,
                        "likedBy" to newLikedBy
                    )
                ).addOnSuccessListener {
                    Log.d("HomeFragment", "Firestore 업데이트 성공")
                    
                    // 현재 outfit 객체의 상태도 업데이트
                    this.currentOutfit = currentOutfit.copy(
                        likeCount = newLikeCount,
                        likedBy = newLikedBy
                    )
                    
                    // UI 업데이트
                    textLikeCount.text = "$newLikeCount"
                    if (isCurrentlyLiked) {
                        iconLike.setImageResource(R.drawable.ic_favorite_border_black_24dp)
                        Log.d("HomeFragment", "좋아요 취소됨 - 빈 하트로 변경")
                    } else {
                        iconLike.setImageResource(R.drawable.ic_favorite_filled_black_24dp)
                        Log.d("HomeFragment", "좋아요 추가됨 - 채워진 하트로 변경")
                    }
                    // 클릭 가능하도록 다시 활성화
                    iconLike.isClickable = true
                }.addOnFailureListener { e ->
                    Log.e("HomeFragment", "좋아요 업데이트 실패", e)
                    Toast.makeText(itemView.context, "좋아요 업데이트 실패", Toast.LENGTH_SHORT).show()
                    // 실패 시에도 클릭 가능하도록 다시 활성화
                    iconLike.isClickable = true
                }
            }

            // Glide를 사용하여 이미지를 로드하는 헬퍼 함수
            private fun loadImageIntoView(imageView: ImageView, imageUrl: String) {
                if (imageUrl.isNotEmpty()) {
                    imageView.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(imageView)
                } else {
                    imageView.visibility = View.GONE
                }
            }
        }
    }
} 