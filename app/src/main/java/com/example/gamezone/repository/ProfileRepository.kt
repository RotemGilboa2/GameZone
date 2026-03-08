package com.example.gamezone.repository

import com.example.gamezone.model.User
import com.example.gamezone.utilities.Constants

// מנהל את פעולות הדאטה של מסך הפרופיל:
// טעינת פרטי המשתמש ועדכון שדות כמו אווטאר, אזור ורשימת משחקים מועדפים דרך UserRepository.
class ProfileRepository(
    private val userRepo: UserRepository = UserRepository()
) {
    fun loadProfile(
        uid: String,
        fallbackDisplayName: String?,
        onComplete: (User?, Exception?) -> Unit
    ) {
        userRepo.getUser(uid) { user, err ->
            if (err != null) return@getUser onComplete(null, err)

            // אם אין מסמך או חסר שם נותנים fallback נקי לUI כדי לא לשבור
            val safe = (user ?: User(uid = uid)).let { u ->
                val name = u.username.ifBlank { fallbackDisplayName ?: "Player" }

                u.copy(
                    username = name,
                    avatarRes = u.avatarRes.ifBlank { "logo" },
                    region = u.region.ifBlank { "EU West" }
                )
            }

            onComplete(safe, null)
        }
    }

    // מעדכן את האווטאר של המשתמש דרך userrepo.
    fun saveAvatar(uid: String, avatarName: String, onComplete: (Exception?) -> Unit) {
        userRepo.updateUser(uid, mapOf(Constants.Firestore.Fields.AVATAR_RES to avatarName), onComplete)
    }

    fun saveRegion(uid: String, region: String, onComplete: (Exception?) -> Unit) {
        userRepo.updateUser(uid, mapOf(Constants.Firestore.Fields.REGION to region), onComplete)
    }

    fun saveGamesCount(uid: String, count: Int, onComplete: (Exception?) -> Unit) {
        userRepo.updateUser(uid, mapOf(Constants.Firestore.Fields.GAMES_COUNT to count), onComplete)
    }

    fun saveFavoriteGames(uid: String, games: List<String>, onComplete: (Exception?) -> Unit) {
        userRepo.updateUser(uid, mapOf(Constants.Firestore.Fields.FAVORITE_GAMES to games), onComplete)
    }
}