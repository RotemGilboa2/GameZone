package com.example.gamezone.model

data class Friend(
    val uid: String = "",
    val username: String = "",
    val avatarRes: String = "logo",
    val createdAt: Long = 0L,
    val online: Boolean = false,
    val lastSeen: Long = 0L
)

