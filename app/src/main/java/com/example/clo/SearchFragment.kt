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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.bumptech.glide.Glide

// 기존 SearchResult 데이터 클래스는 그대로 사용
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

class SearchFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var searchEditText: EditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchAdapter: SearchResultAdapter
    private lateinit var noResultsText: TextView
    private val TAG = "SearchFragment"

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
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        // (액티비티 단에서 처리되거나, 여기서는 프래그먼트 전환 로직에 따라 변경될 수 있음)
        // 현재는 SearchActivity의 로직을 그대로 옮겨옴
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
            return
        }

        initializeViews(view)
        setupSearchFunctionality()
    }

    private fun initializeViews(view: View) {
        searchEditText = view.findViewById(R.id.edit_text_search)
        searchResultsRecyclerView = view.findViewById(R.id.recycler_view_search_results)
        noResultsText = view.findViewById(R.id.text_no_results)

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(context)
        searchAdapter = SearchResultAdapter(emptyList()) { userId ->
            // 검색 결과 클릭 시 FriendProfileActivity로 이동
            val intent = Intent(activity, FriendProfileActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
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
        val currentUserId = auth.currentUser?.uid // 현재 로그인된 사용자 ID 가져오기
        Log.d(TAG, "검색어: $query")
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val searchResults = mutableListOf<SearchResult>()
                Log.d(TAG, "검색된 문서 수: ${documents.size()}")
                for (document in documents) {
                    val userId = document.id // 문서의 ID를 가져옴
                    if (userId == currentUserId) { // 현재 사용자의 ID와 일치하면 건너뛰기
                        Log.d(TAG, "현재 사용자 제외: $userId")
                        continue
                    }

                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: ""
                    Log.d(TAG, "문서 ID: $userId, username: $name, email: $email")

                    // 이름이나 이메일에서 검색어 포함 여부 확인
                    if (name.contains(query, ignoreCase = true) ||
                        email.contains(query, ignoreCase = true)) {
                        Log.d(TAG, "일치하는 사용자 발견: $name")
                        val searchResult = SearchResult(
                            id = document.id,
                            username = name,
                            profileImageUrl = document.getString("profileImageUrl"),
                            codiImageUrl = null, // TODO: 실제 코디 이미지 URL 추가
                            outfitShotUrl = null, // TODO: 실제 아웃핏샷 URL 추가
                            likeCount = 0, // TODO: 실제 좋아요 수 추가
                            description = "사용자", // TODO: 실제 설명 추가
                            followers = (document.get("followers") as? List<String>)?.size ?: 0,
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
        // 이 부분은 액티비티의 생명주기와 다르므로, 메인 액티비티에서 로그인 상태를 관리하는 것이 더 적절합니다.
        // SearchActivity의 로직을 그대로 옮겨놓았지만, 추후 MainActivity에서 처리하는 것을 권장합니다.
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = activity?.getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref?.getBoolean("is_logged_in", false) ?: false
    }

    // SearchResultAdapter 클래스는 SearchActivity에서 가져온 것을 그대로 사용합니다.
    private class SearchResultAdapter(
        private var results: List<SearchResult>,
        private val onItemClick: (String) -> Unit // 클릭 리스너를 위한 람다 함수 추가
    ) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

        fun updateResults(newResults: List<SearchResult>) {
            results = newResults
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
            return SearchResultViewHolder(view, onItemClick)
        }

        override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
            holder.bind(results[position])
        }

        override fun getItemCount(): Int = results.size

        class SearchResultViewHolder(itemView: View, private val onItemClick: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
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
                
                // 프로필 이미지 로드 (Glide 사용)
                if (result.profileImageUrl != null && result.profileImageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(result.profileImageUrl)
                        .placeholder(R.drawable.default_profile_image) // 기본 프로필 이미지 추가
                        .error(R.drawable.default_profile_image) // 오류 시 기본 프로필 이미지
                        .into(imageProfile)
                } else {
                    imageProfile.setImageResource(R.drawable.default_profile_image)
                }

                // TODO: 코디 이미지와 아웃핏샷 이미지 로드
                // result.codiImageUrl이 있다면 imageCodi에 로드
                // result.outfitShotUrl이 있다면 imageOutfitShot에 로드

                itemView.setOnClickListener {
                    onItemClick(result.id) // 클릭 시 userId 전달
                }
            }
        }
    }
} 