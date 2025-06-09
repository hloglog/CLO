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
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

sealed class AdapterItem {
    object Header : AdapterItem()
    data class UserItem(val user: User) : AdapterItem()
}

data class User(
    val id: String,
    val username: String,
    val email: String,
    val profileImageUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0
)

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private val TAG = "HomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = Firebase.auth
        db = Firebase.firestore

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        usersRecyclerView = findViewById(R.id.recycler_view_outfits)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)

        // 헤더와 사용자 데이터를 포함하는 리스트 생성
        val adapterItems = mutableListOf<AdapterItem>()
        adapterItems.add(AdapterItem.Header)

        userAdapter = UserAdapter(adapterItems)
        usersRecyclerView.adapter = userAdapter

        // Firebase에서 사용자 데이터 로드
        loadUsersFromFirebase()

        // 하단 네비게이션 바 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.navigation_home -> {
                    true
                }
                R.id.navigation_mypage -> {
                    val intent = Intent(this, MyPageActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUsersFromFirebase() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val userItems = mutableListOf<AdapterItem>()
                userItems.add(AdapterItem.Header) // 헤더는 항상 첫 번째
                
                for (document in documents) {
                    val user = User(
                        id = document.id,
                        username = document.getString("name") ?: "사용자",
                        email = document.getString("email") ?: "",
                        profileImageUrl = document.getString("profileImageUrl"),
                        followers = document.getLong("followers")?.toInt() ?: 0,
                        following = document.getLong("following")?.toInt() ?: 0
                    )
                    userItems.add(AdapterItem.UserItem(user))
                }
                
                userAdapter.updateItems(userItems)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "사용자 데이터 로드 실패", e)
                Toast.makeText(this, "사용자 데이터 로드 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }

    private class UserAdapter(private var items: List<AdapterItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_USER = 1

        fun updateItems(newItems: List<AdapterItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is AdapterItem.Header -> VIEW_TYPE_HEADER
                is AdapterItem.UserItem -> VIEW_TYPE_USER
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
                VIEW_TYPE_USER -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_outfit, parent, false)
                    UserViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> holder.bind()
                is UserViewHolder -> {
                    val userItem = items[position] as AdapterItem.UserItem
                    holder.bind(userItem.user)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                // 클릭 리스너는 onCreateViewHolder에서 설정됨
            }
        }

        class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageProfile: ImageView = itemView.findViewById(R.id.image_profile)
            private val textUsername: TextView = itemView.findViewById(R.id.text_username)
            private val imageCodi: ImageView = itemView.findViewById(R.id.image_codi)
            private val imageOutfitShot: ImageView = itemView.findViewById(R.id.image_outfit_shot)
            private val textLikeCount: TextView = itemView.findViewById(R.id.text_like_count)
            private val iconLike: ImageView = itemView.findViewById(R.id.icon_like)

            fun bind(user: User) {
                textUsername.text = user.username
                textLikeCount.text = "팔로워 ${user.followers}명"
                
                // TODO: 프로필 이미지 로드 (Glide 등 사용)
                // TODO: 코디 이미지와 아웃핏샷 이미지 로드 (실제 아웃핏 데이터가 있을 때)
            }
        }
    }
} 