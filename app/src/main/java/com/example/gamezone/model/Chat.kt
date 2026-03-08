package com.example.gamezone.model

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),

    val user1Uid: String = "",
    val user2Uid: String = "",

    val user1Name: String = "",
    val user2Name: String = "",

    val user1Avatar: String = "logo",
    val user2Avatar: String = "logo",

    val lastMessage: String = "",
    val lastTimestamp: Long = 0L,

    val lastSenderUid: String = "",

    val lastReadByUser1: Boolean = true,
    val lastReadByUser2: Boolean = true
)
