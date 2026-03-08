package com.example.gamezone.model

data class FriendRequest(
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromAvatarRes: String = "logo",
    val createdAt: Long = 0L
)
