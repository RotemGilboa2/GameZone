package com.example.gamezone.model

data class User(
    val uid: String = "",
    val username: String = "",
    val avatarRes: String = "avatar_01",
    val region: String = "",
    val favoriteGames: List<String> = emptyList(),
    val gamesCount: Int = 0,
    val createdAt: Long = 0L,
    val profileCompleted: Boolean = false,
    val online: Boolean = false,
    val lastSeen: Long = 0L
)
