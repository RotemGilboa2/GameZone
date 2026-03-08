package com.example.gamezone.utilities

object Constants {
    object Firestore {
        const val USERS = "users"
        const val POSTS = "posts"
        const val CHATS = "chats"

        const val FRIEND_REQUESTS = "friend_requests"
        const val FRIENDS = "friends"
        const val SENT_REQUESTS = "sent_requests"

        const val COMMENTS = "comments"
        const val LIKES = "likes"
        const val MESSAGES = "messages"

        object Fields {
            const val USERNAME = "username"
            const val AVATAR_RES = "avatarRes"
            const val REGION = "region"
            const val FAVORITE_GAMES = "favoriteGames"
            const val CREATED_AT = "createdAt"
            const val TIMESTAMP = "timestamp"
            const val LAST_SEEN = "lastSeen"
            const val ONLINE = "online"
            const val LAST_MESSAGE = "lastMessage"
            const val LAST_TIMESTAMP = "lastTimestamp"
            const val LAST_SENDER_UID = "lastSenderUid"
            const val CHAT_ID = "chatId"
            const val PARTICIPANTS = "participants"
            const val USER1_UID = "user1Uid"
            const val USER2_UID = "user2Uid"
            const val LAST_READ_BY_USER1 = "lastReadByUser1"
            const val LAST_READ_BY_USER2 = "lastReadByUser2"

            const val AUTHOR_UID = "authorUid"
            const val AUTHOR_NAME = "authorName"
            const val AUTHOR_AVATAR_RES = "authorAvatarRes"
            const val COMMENT_COUNT = "commentCount"
            const val FROM_UID = "fromUid"
            const val TEXT = "text"

            const val PROFILE_COMPLETED = "profileCompleted"

            const val FROM_USERNAME = "fromUsername"
            const val FROM_AVATAR_RES = "fromAvatarRes"

            const val TO_UID = "toUid"
            const val TO_USERNAME = "toUsername"
            const val TO_AVATAR_RES = "toAvatarRes"
            const val GAMES_COUNT = "gamesCount"

            const val USER1_NAME = "user1Name"
            const val USER2_NAME = "user2Name"

            const val USER1_AVATAR = "user1Avatar"
            const val USER2_AVATAR = "user2Avatar"

            const val LIKE_COUNT = "likeCount"
        }
    }

    object Defaults {
        const val DEFAULT_AVATAR = "avatar_01"
        const val FALLBACK_AVATAR_DRAWABLE = "logo"
    }

    object Intents {
        const val EXTRA_OTHER_UID = "other_uid"
        const val EXTRA_OTHER_NAME = "other_name"
        const val EXTRA_POST_ID = "postId"
    }

    object Resources {
        const val DRAWABLE = "drawable"
    }

    object Formats {
        const val CHAT_TIME = "h:mm a"
        const val MESSAGE_TIME_24H = "HH:mm"
    }

    object Debug {
        const val BINDING_OUTSIDE_LIFECYCLE = "Binding accessed outside of view lifecycle."
    }

    object UiTags {
        const val AVATAR_PICKER = "AvatarPicker"
    }

}