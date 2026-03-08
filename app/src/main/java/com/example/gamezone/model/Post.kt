package com.example.gamezone.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val text: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatarRes: String = "logo",
    val createdAt: Timestamp? = null,
    val likeCount: Long = 0,
    val commentCount: Long = 0
)
