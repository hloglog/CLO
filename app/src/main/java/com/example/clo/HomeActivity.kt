package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.ktx.Firebase
import com.example.clo.AdapterItem
import com.example.clo.Outfit

class HomeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var outfitsRecyclerView: RecyclerView
    private lateinit var outfitAdapter: OutfitAdapter
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = Firebase.auth

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        outfitsRecyclerView = findViewById(R.id.recycler_view_outfits)
        outfitsRecyclerView.layoutManager = LinearLayoutManager(this)

        // 더미 데이터 생성
        val dummyOutfits = listOf(
            Outfit(
                id = "1",
                username = "3han",
                profileImageUrl = "", // TODO: 실제 이미지 URL로 변경
                codiImageUrl = "", // TODO: 실제 이미지 URL로 변경
                outfitShotUrl = "", // TODO: 실제 이미지 URL로 변경
                likeCount = 123
            ),
            Outfit(
                id = "2",
                username = "User2",
                profileImageUrl = "", // TODO: 실제 이미지 URL로 변경
                codiImageUrl = "", // TODO: 실제 이미지 URL로 변경
                outfitShotUrl = "", // TODO: 실제 이미지 URL로 변경
                likeCount = 55
            ),
            Outfit(
                id = "3",
                username = "Fashionista",
                profileImageUrl = "", // TODO: 실제 이미지 URL로 변경
                codiImageUrl = "", // TODO: 실제 이미지 URL로 변경
                outfitShotUrl = "", // TODO: 실제 이미지 URL로 변경
                likeCount = 201
            ),
            Outfit(
                id = "4",
                username = "StyleGuy",
                profileImageUrl = "", // TODO: 실제 이미지 URL로 변경
                codiImageUrl = "", // TODO: 실제 이미지 URL로 변경
                outfitShotUrl = "", // TODO: 실제 이미지 URL로 변경
                likeCount = 88
            ),
             Outfit(
                id = "5",
                username = "OutfitQueen",
                profileImageUrl = "", // TODO: 실제 이미지 URL로 변경
                codiImageUrl = "", // TODO: 실제 이미지 URL로 변경
                outfitShotUrl = "", // TODO: 실제 이미지 URL로 변경
                likeCount = 340
            )
        )

        // 헤더와 더미 착장 데이터를 포함하는 리스트 생성
        val adapterItems = mutableListOf<AdapterItem>()
        adapterItems.add(AdapterItem.Header)
        dummyOutfits.map { AdapterItem.OutfitItem(it) }.let { adapterItems.addAll(it) }

        outfitAdapter = OutfitAdapter(adapterItems)
        outfitsRecyclerView.adapter = outfitAdapter

        // 하단 네비게이션 바 설정
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.menu_home

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_search -> {
                    Toast.makeText(this, "홈에서 검색 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_home -> {
                    true
                }
                R.id.menu_mypage -> {
                    val intent = Intent(this, MyPageFragment::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
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

    private class OutfitAdapter(private val items: List<AdapterItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_OUTFIT = 1

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is AdapterItem.Header -> VIEW_TYPE_HEADER
                is AdapterItem.OutfitItem -> VIEW_TYPE_OUTFIT
                else -> VIEW_TYPE_OUTFIT // UserItem 등 예외는 Outfit으로 처리
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
                VIEW_TYPE_OUTFIT -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_outfit, parent, false)
                    OutfitViewHolder(view)
                }
                else -> throw IllegalArgumentException("Invalid view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderViewHolder -> holder.bind()
                is OutfitViewHolder -> {
                    val outfitItem = items[position] as? AdapterItem.OutfitItem
                    outfitItem?.let { holder.bind(it.outfit) }
                }
                else -> {}
            }
        }

        override fun getItemCount(): Int = items.size

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                // 클릭 리스너는 onCreateViewHolder에서 설정됨
            }
        }

        class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageProfile: ImageView = itemView.findViewById(R.id.image_profile)
            private val textUsername: TextView = itemView.findViewById(R.id.text_username)
            private val topImage: ImageView = itemView.findViewById(R.id.top_image)
            private val bottomImage: ImageView = itemView.findViewById(R.id.bottom_image)
            private val shoesImage: ImageView = itemView.findViewById(R.id.shoes_image)
            private val accessoriesImage: ImageView = itemView.findViewById(R.id.accessories_image)
            private val outfitShotImage: ImageView = itemView.findViewById(R.id.outfit_shot_image)
            private val textLikeCount: TextView = itemView.findViewById(R.id.text_like_count)
            private val iconLike: ImageView = itemView.findViewById(R.id.icon_like)

            fun bind(outfit: Outfit) {
                textUsername.text = outfit.username
                textLikeCount.text = outfit.likeCount.toString()
                
                // TODO: 실제 이미지 로드 로직 추가
                // 현재는 더미 데이터이므로 실제 이미지 로드가 필요
            }
        }
    }
} 