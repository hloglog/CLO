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

sealed class AdapterItem {
    object Header : AdapterItem()
    data class OutfitItem(val outfit: Outfit) : AdapterItem()
}

data class Outfit(
    val id: String,
    val username: String,
    val profileImageUrl: String? = null,
    val codiImageUrl: String? = null,
    val outfitShotUrl: String? = null,
    val likeCount: Int = 0
)

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

        // 기존 요소들은 숨김 처리 (주석 처리 해제)
//        findViewById<TextView>(R.id.tvUserEmail)?.visibility = View.GONE
//        findViewById<Button>(R.id.btnLogout)?.visibility = View.GONE

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
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_home // 홈 아이템 선택 상태로 표시

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_search -> {
                    // TODO: 검색 화면으로 이동
                    Toast.makeText(this, "홈에서 검색 클릭", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_home -> {
                    // 현재 화면
                    true
                }
                R.id.navigation_mypage -> {
                    // TODO: 마이페이지 화면으로 이동
                    val intent = Intent(this, MyPageActivity::class.java)
                    startActivity(intent)
                    finish() // 현재 액티비티 종료
                    true
                }
                else -> false
            }
        }

        // "오늘의 착장 업로드" 카드뷰 클릭 리스너는 어댑터로 이동
//        findViewById<androidx.cardview.widget.CardView>(R.id.card_upload_outfit).setOnClickListener {
//            // TODO: 착장 업로드 화면으로 이동하는 코드 추가
//            Toast.makeText(this, "착장 업로드 버튼 클릭", Toast.LENGTH_SHORT).show()
//        }
    }

    override fun onStart() {
        super.onStart()
        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // SharedPreferences에서 로그인 상태 확인
    private fun isUserLoggedIn(): Boolean {
        val sharedPref = getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("is_logged_in", false)
    }

    // RecyclerView 어댑터
    private class OutfitAdapter(private val items: List<AdapterItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val VIEW_TYPE_HEADER = 0
        private val VIEW_TYPE_OUTFIT = 1

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is AdapterItem.Header -> VIEW_TYPE_HEADER
                is AdapterItem.OutfitItem -> VIEW_TYPE_OUTFIT
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VIEW_TYPE_HEADER -> {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_upload_header, parent, false)
                    HeaderViewHolder(view)
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
                    val outfitItem = items[position] as AdapterItem.OutfitItem
                    holder.bind(outfitItem.outfit)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind() {
                val cardUploadOutfit = itemView.findViewById<CardView>(R.id.card_upload_outfit)
                cardUploadOutfit.setOnClickListener { v ->
                    // TODO: 착장 업로드 화면으로 이동하는 코드 추가
                    Toast.makeText(v.context, "착장 업로드 버튼 클릭 (어댑터)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        class OutfitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageProfile: ImageView = itemView.findViewById(R.id.image_profile)
            private val textUsername: TextView = itemView.findViewById(R.id.text_username)
            private val imageCodi: ImageView = itemView.findViewById(R.id.image_codi)
            private val imageOutfitShot: ImageView = itemView.findViewById(R.id.image_outfit_shot)
            private val textLikeCount: TextView = itemView.findViewById(R.id.text_like_count)
            private val iconLike: ImageView = itemView.findViewById(R.id.icon_like)

            fun bind(outfit: Outfit) {
                // TODO: 이미지 로딩 라이브러리 (예: Glide)를 사용하여 이미지 로드
                // Glide.with(itemView.context).load(outfit.profileImageUrl).into(imageProfile)
                // Glide.with(itemView.context).load(outfit.codiImageUrl).into(imageCodi)
                // Glide.with(itemView.context).load(outfit.outfitShotUrl).into(imageOutfitShot)

                textUsername.text = outfit.username
                textLikeCount.text = outfit.likeCount.toString()

                // TODO: 좋아요 버튼 클릭 리스너 추가
            }
        }
    }
} 