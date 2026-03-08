package com.example.gamezone.repository

import com.example.gamezone.model.User
import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.utilities.Constants
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

// מנהל את פעולות הדאטה של מסך ה-Discover מול Firestore: האזנה בזמן אמת לחברים ולבקשות חברות,
// חיפוש משתמשים לפי שם/משחק מועדף, ושליחת בקשות חברות תוך עדכון רשימת הבקשות אצל שני הצדדים.
class DiscoverRepository(
    private val fs: FirestoreService = FirestoreService(),
) {

    fun listenFriends(myUid: String, onUpdate: (QuerySnapshot) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return fs.friendsCollection(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap != null) onUpdate(snap)
            }
    }

    fun listenIncoming(myUid: String, onUpdate: (QuerySnapshot) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return fs.friendRequestsCollection(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap != null) onUpdate(snap)
            }
    }

    fun listenSent(myUid: String, onUpdate: (QuerySnapshot) -> Unit, onError: (Exception) -> Unit): ListenerRegistration {
        return fs.sentRequestsCollection(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap != null) onUpdate(snap)
            }
    }

    fun searchUsers(
        queryText: String,
        selectedGame: String?,
        excludedUids: Set<String>,
        onComplete: (List<User>, Exception?) -> Unit
    ) {
        val game = selectedGame
        val q = queryText.trim()
        val qLower = q.lowercase()

        val query = when {
            !game.isNullOrBlank() -> {
                fs.usersCollection()
                    .whereArrayContains(Constants.Firestore.Fields.FAVORITE_GAMES, game)
                    .limit(200)
            }
            q.isNotEmpty() -> {
                fs.usersCollection()
                    .orderBy(Constants.Firestore.Fields.USERNAME)
                    .startAt(q)
                    .endAt(q + "\uf8ff")
                    .limit(50)
            }
            else -> {
                fs.usersCollection().limit(50)
            }
        }

        query.get()
            .addOnSuccessListener { documents ->
                val usersList = mutableListOf<User>()

                for (doc in documents) {
                    val uid = doc.id
                    if (excludedUids.contains(uid)) continue

                    val username = doc.getString(Constants.Firestore.Fields.USERNAME) ?: "Player"

                    // אם יש גם משחק וגם טקסט - סינון שם בצד לקוח
                    if (!game.isNullOrBlank() && qLower.isNotEmpty() && !username.lowercase()
                        .startsWith(qLower)) {
                        continue
                    }

                    val avatarRes = doc.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"

                    val user = User(
                        uid = uid,
                        username = username,
                        avatarRes = avatarRes,
                        region = doc.getString(Constants.Firestore.Fields.REGION) ?: "",
                        favoriteGames = (doc.get(Constants.Firestore.Fields.FAVORITE_GAMES)
                                as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = doc.getLong(Constants.Firestore.Fields.CREATED_AT) ?: 0L,
                        profileCompleted = doc.getBoolean(Constants.Firestore.Fields.PROFILE_COMPLETED)
                            ?: false
                    )

                    usersList.add(user)
                }

                onComplete(usersList, null)
            }
            .addOnFailureListener { e ->
                onComplete(emptyList(), e)
            }
    }

    // שולח בקשה אצלי נשמר בsent ואצל הצד השני בfriend
    fun sendFriendRequest(
        myUid: String,
        userToAdd: User,
        onComplete: (Exception?) -> Unit
    ) {
        fs.usersCollection().document(myUid).get()
            .addOnSuccessListener { meDoc ->
                val myUsername = meDoc.getString(Constants.Firestore.Fields.USERNAME) ?: "Player"
                val myAvatarRes = meDoc.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"
                val now = System.currentTimeMillis()

                val requestMap = mapOf(
                    Constants.Firestore.Fields.FROM_UID to myUid,
                    Constants.Firestore.Fields.FROM_USERNAME to myUsername,
                    Constants.Firestore.Fields.FROM_AVATAR_RES to myAvatarRes,
                    Constants.Firestore.Fields.CREATED_AT to now
                )

                fs.friendRequestsCollection(userToAdd.uid).document(myUid)
                    .set(requestMap)
                    .addOnSuccessListener {

                        val sentMap = mapOf(
                            Constants.Firestore.Fields.TO_UID to userToAdd.uid,
                            Constants.Firestore.Fields.TO_USERNAME to userToAdd.username,
                            Constants.Firestore.Fields.TO_AVATAR_RES to userToAdd.avatarRes,
                            Constants.Firestore.Fields.CREATED_AT to now
                        )

                        fs.sentRequestsCollection(myUid).document(userToAdd.uid)
                            .set(sentMap)
                            .addOnSuccessListener { onComplete(null) }
                            .addOnFailureListener { e -> onComplete(e) }
                    }
                    .addOnFailureListener { e -> onComplete(e) }
            }
            .addOnFailureListener { e -> onComplete(e) }
    }
}