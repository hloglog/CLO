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

// 기존 AdapterItem, User 데이터 클래스는 그대로 사용
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 현재 로그인된 사용자가 없거나 SharedPreferences에 로그인 상태가 false이면 로그인 화면으로 이동
        // (액티비티 단에서 처리되거나, 여기서는 프래그먼트 전환 로직에 따라 변경될 수 있음)
        // 현재는 HomeActivity의 로직을 그대로 옮겨옴
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

        loadUsersFromFirebase()
    }

    private fun loadUsersFromFirebase() {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                val userItems = mutableListOf<AdapterItem>()
                userItems.add(AdapterItem.Header)
                
                for (document in documents) {
                    val user = User(
                        id = document.id,
                        username = document.getString("name") ?: "사용자",
                        email = document.getString("email") ?: "",
                        profileImageUrl = document.getString("profileImageUrl"),
                        followers = (document.get("followers") as? List<String>)?.size ?: 0,
                        following = document.getLong("following")?.toInt() ?: 0
                    )
                    userItems.add(AdapterItem.UserItem(user))
                }
                
                userAdapter.updateItems(userItems)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "사용자 데이터 로드 실패", e)
                Toast.makeText(context, "사용자 데이터 로드 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStart() {
        super.onStart()
        // 이 부분은 액티비티의 생명주기와 다르므로, 메인 액티비티에서 로그인 상태를 관리하는 것이 더 적절합니다.
        // HomeActivity의 로직을 그대로 옮겨놓았지만, 추후 MainActivity에서 처리하는 것을 권장합니다.
        if (auth.currentUser == null || !isUserLoggedIn()) {
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPref = activity?.getSharedPreferences("login_status", Context.MODE_PRIVATE)
        return sharedPref?.getBoolean("is_logged_in", false) ?: false
    }

    // UserAdapter 클래스는 HomeActivity에서 가져온 것을 그대로 사용합니다.
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