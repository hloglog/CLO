package com.example.clo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore

class FriendProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var followersTextView: TextView
    private lateinit var followButton: Button
    
    // TODAY 섹션 이미지뷰들
    private lateinit var todayTopImage: ImageView
    private lateinit var todayBottomImage: ImageView
    private lateinit var todayShoesImage: ImageView
    private lateinit var todayAccessoriesImage: ImageView

    private var isFollowing = false
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String? = null
    private var friendUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_profile)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid

        profileImageView = findViewById(R.id.profileImageView)
        usernameTextView = findViewById(R.id.usernameTextView)
        followersTextView = findViewById(R.id.followersTextView)
        followButton = findViewById(R.id.followButton)
        
        // TODAY 섹션 이미지뷰들 초기화
        todayTopImage = findViewById(R.id.todayTopImage)
        todayBottomImage = findViewById(R.id.todayBottomImage)
        todayShoesImage = findViewById(R.id.todayShoesImage)
        todayAccessoriesImage = findViewById(R.id.todayAccessoriesImage)

        // Get user ID from intent (e.g., from SearchActivity)
        friendUserId = intent.getStringExtra("userId")

        if (friendUserId != null && currentUserId != null) {
            loadFriendProfile(friendUserId!!)
        } else {
            Log.e("FriendProfileActivity", "User ID is null. Cannot load profile.")
            // Handle case where userId is not provided, maybe show an error or go back
            finish()
        }

        followButton.setOnClickListener {
            toggleFollowStatus()
        }
    }

    private fun loadFriendProfile(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("name") ?: "Unknown User"
                    val profileImageUrl = document.getString("profileImageUrl")
                    
                    // 팔로워 리스트 가져오기
                    val followersList = document.get("followers") as? List<String> ?: emptyList()
                    val followersCount = followersList.size

                    usernameTextView.text = username
                    followersTextView.text = "${followersCount} followers"
                    
                    if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
                        Glide.with(this).load(profileImageUrl).into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile_image) // Fallback image
                    }

                    // 현재 사용자가 이 친구를 팔로우하고 있는지 확인
                    isFollowing = followersList.contains(currentUserId)
                    updateFollowButtonUI()
                    
                    // 친구의 오늘 착장 로드
                    loadFriendTodayOutfit(userId)

                } else {
                    Log.d("FriendProfileActivity", "No such document")
                    // Handle case where user not found
                    usernameTextView.text = "User Not Found"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FriendProfileActivity", "Error getting user profile: ", exception)
                usernameTextView.text = "Error loading profile"
            }
    }

    private fun loadFriendTodayOutfit(userId: String) {
        // 인덱스 없이 작동하도록 단순한 쿼리 사용
        firestore.collection("today")
            .whereEqualTo("userId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    
                    // 오늘 날짜인지 확인 (클라이언트에서 필터링)
                    val timestamp = document.getTimestamp("timestamp")
                    val isToday = isTimestampToday(timestamp)
                    
                    if (isToday) {
                        // 각 이미지 로드
                        loadImageIntoView(todayTopImage, document.getString("topImageUrl"))
                        loadImageIntoView(todayBottomImage, document.getString("bottomImageUrl"))
                        loadImageIntoView(todayShoesImage, document.getString("shoesImageUrl"))
                        loadImageIntoView(todayAccessoriesImage, document.getString("accessoriesImageUrl"))
                        
                        Log.d("FriendProfileActivity", "친구의 오늘 착장 로드 완료")
                    } else {
                        Log.d("FriendProfileActivity", "친구의 오늘 착장이 아닙니다")
                        setDefaultImages()
                    }
                } else {
                    Log.d("FriendProfileActivity", "친구의 오늘 착장이 없습니다")
                    // 오늘 착장이 없는 경우 기본 이미지 표시
                    setDefaultImages()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FriendProfileActivity", "오늘 착장 로드 실패", e)
                setDefaultImages()
            }
    }
    
    private fun isTimestampToday(timestamp: com.google.firebase.Timestamp?): Boolean {
        if (timestamp == null) return false
        
        val calendar = java.util.Calendar.getInstance()
        val today = java.util.Calendar.getInstance()
        
        calendar.time = timestamp.toDate()
        
        return calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
               calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
    }
    
    private fun loadImageIntoView(imageView: ImageView, imageUrl: String?) {
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            imageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView)
        } else {
            imageView.visibility = View.GONE
        }
    }
    
    private fun setDefaultImages() {
        // 오늘 착장이 없을 때 기본 이미지 표시
        todayTopImage.setImageResource(R.drawable.placeholder_image)
        todayBottomImage.setImageResource(R.drawable.placeholder_image)
        todayShoesImage.setImageResource(R.drawable.placeholder_image)
        todayAccessoriesImage.setImageResource(R.drawable.placeholder_image)
    }

    private fun toggleFollowStatus() {
        if (friendUserId == null || currentUserId == null) return

        val userRef = firestore.collection("users").document(friendUserId!!)
        
        if (isFollowing) {
            // 언팔로우: 팔로워 리스트에서 현재 사용자 제거
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentFollowers = snapshot.get("followers") as? List<String> ?: emptyList()
                val newFollowers = currentFollowers.toMutableList()
                newFollowers.remove(currentUserId)
                transaction.update(userRef, "followers", newFollowers)
                null
            }.addOnSuccessListener { 
                Log.d("FriendProfileActivity", "Unfollowed successfully.")
                isFollowing = false
                updateFollowButtonUI()
                updateFollowersCount()
            }.addOnFailureListener { e -> 
                Log.e("FriendProfileActivity", "Failed to unfollow.", e) 
            }
        } else {
            // 팔로우: 팔로워 리스트에 현재 사용자 추가
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentFollowers = snapshot.get("followers") as? List<String> ?: emptyList()
                val newFollowers = currentFollowers.toMutableList()
                if (!newFollowers.contains(currentUserId)) {
                    newFollowers.add(currentUserId!!)
                }
                transaction.update(userRef, "followers", newFollowers)
                null
            }.addOnSuccessListener { 
                Log.d("FriendProfileActivity", "Followed successfully.")
                isFollowing = true
                updateFollowButtonUI()
                updateFollowersCount()
            }.addOnFailureListener { e -> 
                Log.e("FriendProfileActivity", "Failed to follow.", e) 
            }
        }
    }

    private fun updateFollowersCount() {
        if (friendUserId == null) return
        
        firestore.collection("users").document(friendUserId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val followersList = document.get("followers") as? List<String> ?: emptyList()
                    val followersCount = followersList.size
                    followersTextView.text = "${followersCount} followers"
                }
            }
    }

    private fun updateFollowButtonUI() {
        if (isFollowing) {
            followButton.text = "FOLLOWING"
            followButton.setBackgroundResource(R.drawable.button_background_dark) // Dark background
            followButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            followButton.text = "FOLLOW"
            followButton.setBackgroundResource(R.drawable.button_background_dark) // White border
            followButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }
} 