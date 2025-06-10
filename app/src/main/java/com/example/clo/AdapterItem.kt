package com.example.clo

sealed class AdapterItem {
    object Header : AdapterItem()
    data class OutfitItem(val outfit: Outfit) : AdapterItem()
    data class UserItem(val user: User) : AdapterItem()
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