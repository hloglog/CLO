package com.example.clo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class SearchResult(
    val id: String,
    val username: String,
    val profileImageUrl: String? = null,
    val codiImageUrl: String? = null,
    val outfitShotUrl: String? = null,
    val likeCount: Int = 0,
    val description: String = "",
    val followers: Int = 0,
    val following: Int = 0
)

class SearchActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var searchEditText: EditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var noResultsText: TextView
    private val TAG = "SearchActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        auth = Firebase.auth
        db = Firebase.firestore

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupSearchFunctionality()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        searchEditText = findViewById(R.id.edit_text_search)
        searchResultsRecyclerView = findViewById(R.id.recycler_view_search_results)
        noResultsText = findViewById(R.id.text_no_results)
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchAdapter = SearchResultAdapter(emptyList())
        searchResultsRecyclerView.adapter = searchAdapter
    }

    private fun setupSearchFunctionality() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                performSearch(s.toString())
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.navigation_search

        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_search -> {
                    true
                }
                R.id.navigation_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
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

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            searchAdapter.updateResults(emptyList())
            showNoResults(false)
            return
        }

        // Firebase에서 사용자 검색
        searchUsersFromFirebase(query)
    }

    private fun searchUsersFromFirebase(query: String) {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val searchResults = mutableListOf<SearchResult>()
                
                for (document in documents) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    
                    // 이름이나 이메일에서 검색어 포함 여부 확인
                    if (name.contains(query, ignoreCase = true) || 
                        email.contains(query, ignoreCase = true)) {
                        
                        val searchResult = SearchResult(
                            id = document.id,
                            username = name,
                            profileImageUrl = document.getString("profileImageUrl"),
                            codiImageUrl = null, // TODO: 실제 코디 이미지 URL 추가
                            outfitShotUrl = null, // TODO: 실제 아웃핏샷 URL 추가
                            likeCount = 0, // TODO: 실제 좋아요 수 추가
                            description = "사용자", // TODO: 실제 설명 추가
                            followers = document.getLong("followers")?.toInt() ?: 0,
                            following = document.getLong("following")?.toInt() ?: 0
                        )
                        searchResults.add(searchResult)
                    }
                }
                
                searchAdapter.updateResults(searchResults)
                showNoResults(searchResults.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "사용자 검색 실패", e)
                searchAdapter.updateResults(emptyList())
                showNoResults(true)
            }
    }

    private fun showNoResults(show: Boolean) {
        noResultsText.visibility = if (show) View.VISIBLE else View.GONE
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

    private class SearchResultAdapter(private var results: List<SearchResult>) : 
        RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

        fun updateResults(newResults: List<SearchResult>) {
            results = newResults
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
            return SearchResultViewHolder(view)
        }

        override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
            holder.bind(results[position])
        }

        override fun getItemCount(): Int = results.size

        class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageProfile: ImageView = itemView.findViewById(R.id.image_profile)
            private val textUsername: TextView = itemView.findViewById(R.id.text_username)
            private val textDescription: TextView = itemView.findViewById(R.id.text_description)
            private val imageCodi: ImageView = itemView.findViewById(R.id.image_codi)
            private val imageOutfitShot: ImageView = itemView.findViewById(R.id.image_outfit_shot)
            private val textLikeCount: TextView = itemView.findViewById(R.id.text_like_count)
            private val iconLike: ImageView = itemView.findViewById(R.id.icon_like)

            fun bind(result: SearchResult) {
                textUsername.text = result.username
                textDescription.text = "팔로워 ${result.followers}명"
                textLikeCount.text = result.likeCount.toString()
                
                // TODO: 프로필 이미지 로드 (Glide 등 사용)
                // TODO: 코디 이미지와 아웃핏샷 이미지 로드
            }
        }
    }
} 