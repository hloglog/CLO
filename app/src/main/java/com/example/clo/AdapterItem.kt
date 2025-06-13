package com.example.clo

sealed class AdapterItem {
    object Header : AdapterItem()
    object Spacer : AdapterItem()
    data class OutfitItem(val outfit: Outfit) : AdapterItem()
    data class UserItem(val user: User) : AdapterItem()
    data class TodayOutfitItem(val outfit: TodayOutfit) : AdapterItem()
}

data class Outfit(
    val id: String,
    val username: String,
    val profileImageUrl: String? = null,
    val codiImageUrl: String? = null,
    val outfitShotUrl: String? = null,
    val likeCount: Int = 0
)

data class User(
    val id: String,
    val username: String,
    val email: String,
    val profileImageUrl: String? = null,
    val followers: Int = 0,
    val following: Int = 0
)

// TodayOutfit 데이터 클래스 (HomeFragment에서 사용)
data class TodayOutfit(
    val id: String,
    val userId: String,
    val topImageUrl: String,
    val bottomImageUrl: String,
    val shoesImageUrl: String,
    val accessoriesImageUrl: String,
    val outfitShotUrl: String? = null,
    val timestamp: com.google.firebase.Timestamp,
    val username: String = "",
    val profileImageUrl: String? = null,
    val likeCount: Int = 0,
    val likedBy: List<String> = emptyList()
) 