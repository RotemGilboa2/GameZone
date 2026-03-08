package com.example.gamezone.repository

import com.example.gamezone.model.Friend
import com.example.gamezone.model.FriendRequest
import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.utilities.Constants
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

// שכבת הדאטה של מסך החברים (“Squad”):
//הוא מנהל מול Firestore את כל מה שקשור לחברים ובקשות חברות — טעינה, האזנה בזמן אמת,
// באדג’ של בקשות, נוכחות (Online/Last seen), ואישור/דחייה של בקשות בצורה עקבית עם Batch.
class SquadRepository(
    private val fs: FirestoreService = FirestoreService()
) {


    // מאזין לבקשות נכנסות ומחזיר את הכמות (באדג') בשביל אייקון אדום עם מספר
    fun listenRequestsBadge(
        myUid: String,
        onUpdate: (count: Int) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return fs.friendRequestsCollection(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap == null) return@addSnapshotListener
                onUpdate(snap.size())
            }
    }

    fun loadFriends(
        myUid: String,
        onComplete: (List<Friend>, Exception?) -> Unit
    ) {
        fs.friendsCollection(myUid).get()
            .addOnSuccessListener { snap ->
                val list = snap.toObjects(Friend::class.java)
                    .sortedByDescending { it.createdAt }
                onComplete(list, null)
            }
            .addOnFailureListener { e -> onComplete(emptyList(), e) }
    }

    fun loadRequests(
        myUid: String,
        onComplete: (List<FriendRequest>, Exception?) -> Unit
    ) {
        fs.friendRequestsCollection(myUid).get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.map { doc ->
                    val fromUid = doc.id
                    FriendRequest(
                        fromUid = fromUid,
                        fromUsername = doc.getString(Constants.Firestore.Fields.FROM_USERNAME) ?: "Player",
                        fromAvatarRes = doc.getString(Constants.Firestore.Fields.FROM_AVATAR_RES) ?: "logo",
                        createdAt = doc.getLong(Constants.Firestore.Fields.CREATED_AT) ?: 0L
                    )
                }.sortedByDescending { it.createdAt }

                onComplete(list, null)
            }
            .addOnFailureListener { e -> onComplete(emptyList(), e) }
    }

    fun listenFriendPresence(
        friendUid: String,
        onUpdate: (online: Boolean, lastSeen: Long, avatarRes: String?, username: String?) -> Unit
    ): ListenerRegistration {
        return fs.usersCollection().document(friendUid)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener

                val online = snap.getBoolean(Constants.Firestore.Fields.ONLINE) ?: false
                val lastSeen = snap.getLong(Constants.Firestore.Fields.LAST_SEEN) ?: 0L
                val avatarRes = snap.getString(Constants.Firestore.Fields.AVATAR_RES)
                val username = snap.getString(Constants.Firestore.Fields.USERNAME)

                onUpdate(online, lastSeen, avatarRes, username)
            }
    }


    fun syncFriendsUsersData(myUid: String, friendsList: List<Friend>) {
        val unique = HashSet<String>()

        for (f in friendsList) {
            if (f.uid.isBlank()) continue
            if (!unique.add(f.uid)) continue

            fs.usersCollection().document(f.uid).get()
                .addOnSuccessListener { userSnap ->
                    val freshName = userSnap.getString(Constants.Firestore.Fields.USERNAME) ?: f.username
                    val freshAvatar = userSnap.getString(Constants.Firestore.Fields.AVATAR_RES) ?: f.avatarRes

                    val needsUpdate = (freshName != f.username) || (freshAvatar != f.avatarRes)
                    if (!needsUpdate) return@addOnSuccessListener

                    fs.friendsCollection(myUid).document(f.uid)
                        .set(
                            mapOf(
                                Constants.Firestore.Fields.USERNAME to freshName,
                                Constants.Firestore.Fields.AVATAR_RES to freshAvatar
                            ),
                            SetOptions.merge()
                        )
                }
        }
    }


    //      אישור בקשת חברות:
    //      1) מוסיף את השולח לרשימת החברים שלי
    //      2) מוסיף אותי לרשימת החברים של השולח
    //      3) מוחק את הבקשה הנכנסת אצלי
    //      4) מוחק את הבקשה שנשלחה אצל השולח (sent_requests)
    //
    fun acceptRequestFull(
        myUid: String,
        req: FriendRequest,
        onComplete: (Exception?) -> Unit
    ) {
        val senderUid = req.fromUid
        val now = System.currentTimeMillis()

        // מביאים את הפרופיל שלי כדי לשמור אצל הצד השני (שם/אווטאר)
        fs.usersCollection().document(myUid).get()
            .addOnSuccessListener { meSnap ->
                val myUsername = meSnap.getString(Constants.Firestore.Fields.USERNAME) ?: "Player"
                val myAvatar = meSnap.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"

                val batch = fs.usersCollection().firestore.batch()

                val friendForMe = Friend(
                    uid = senderUid,
                    username = req.fromUsername,
                    avatarRes = req.fromAvatarRes,
                    createdAt = now
                )

                val friendForOther = Friend(
                    uid = myUid,
                    username = myUsername,
                    avatarRes = myAvatar,
                    createdAt = now
                )

                val meFriendRef = fs.friendsCollection(myUid).document(senderUid)
                val otherFriendRef = fs.friendsCollection(senderUid).document(myUid)
                val incomingReqRef = fs.friendRequestsCollection(myUid).document(senderUid)
                val senderSentRef = fs.sentRequestsCollection(senderUid).document(myUid)

                batch.set(meFriendRef, friendForMe, SetOptions.merge())
                batch.set(otherFriendRef, friendForOther, SetOptions.merge())
                batch.delete(incomingReqRef)
                batch.delete(senderSentRef)

                batch.commit()
                    .addOnSuccessListener { onComplete(null) }
                    .addOnFailureListener { onComplete(it) }
            }
            .addOnFailureListener { onComplete(it) }
    }

    //      דחיית בקשת חברות:
    //      מוחק את הבקשה הנכנסת + את הבקשה שנשלחה אצל השולח,
    fun declineRequestFull(
        myUid: String,
        req: FriendRequest,
        onComplete: (Exception?) -> Unit
    ) {
        val senderUid = req.fromUid
        val batch = fs.usersCollection().firestore.batch()

        val incomingReqRef = fs.friendRequestsCollection(myUid).document(senderUid)
        val senderSentRef = fs.sentRequestsCollection(senderUid).document(myUid)

        // ניקוי שאריות אפשריות (אם גם אני שלחתי לו בקשה במקביל)
        val mySentToSender = fs.sentRequestsCollection(myUid).document(senderUid)
        val senderIncomingFromMe = fs.friendRequestsCollection(senderUid).document(myUid)

        batch.delete(incomingReqRef)
        batch.delete(senderSentRef)
        batch.delete(mySentToSender)
        batch.delete(senderIncomingFromMe)

        batch.commit()
            .addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { onComplete(it) }
    }
}