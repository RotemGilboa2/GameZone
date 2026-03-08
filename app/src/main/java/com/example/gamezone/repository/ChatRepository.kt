package com.example.gamezone.repository

import com.example.gamezone.model.Chat
import com.example.gamezone.model.Message
import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.utilities.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions


//מנהל את כל פעולות הצ’אט מול Firestore:
// יצירת צ’אט אם חסר, האזנה להודעות בזמן אמת, שליחת הודעות,
// עדכון הודעה אחרונה וסימוני נקרא/לא נקרא, והצגת סטטוס נוכחות.
class ChatRepository(
    private val fs: FirestoreService = FirestoreService()
) {

    // מונע צאט כפול שאם A או B יצר אז זה יהיה אותו צאט
    fun buildChatId(a: String, b: String): String =
        if (a < b) "${a}_$b" else "${b}_$a"


    // Creates chat doc if missing + returns resolved other name (from DB if exists)
    fun ensureChatDocExists(
        myUid: String,
        otherUid: String,
        fallbackOtherName: String,
        onOtherNameResolved: (String) -> Unit,
        onComplete: (Exception?) -> Unit
    ) {
        val chatId = buildChatId(myUid, otherUid)
        val chatRef = fs.chatsCollection().document(chatId)

        fs.usersCollection().document(myUid).get()
            .addOnSuccessListener { meSnap ->
                val myName = meSnap.getString(Constants.Firestore.Fields.USERNAME) ?: "Me"
                val myAvatar = meSnap.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"

                fs.usersCollection().document(otherUid).get()
                    .addOnSuccessListener { otherSnap ->
                        val otherAvatar = otherSnap.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"
                        val otherNameFromDb = otherSnap.getString(Constants.Firestore.Fields.USERNAME) ?: fallbackOtherName

                        onOtherNameResolved(otherNameFromDb) // מחזיר לUI את השם של הצד השני

                        chatRef.get() // בדיקה אם הצאט קיים
                            .addOnSuccessListener { existing ->
                                if (!existing.exists()) {
                                    val user1Uid = if (myUid < otherUid) myUid else otherUid
                                    val user2Uid = if (myUid < otherUid) otherUid else myUid

                                    val chatDoc = Chat(
                                        chatId = chatId,
                                        participants = listOf(myUid, otherUid),

                                        user1Uid = user1Uid,
                                        user2Uid = user2Uid,

                                        user1Name = if (myUid < otherUid) myName else otherNameFromDb,
                                        user2Name = if (myUid < otherUid) otherNameFromDb else myName,

                                        user1Avatar = if (myUid < otherUid) myAvatar else otherAvatar,
                                        user2Avatar = if (myUid < otherUid) otherAvatar else myAvatar,

                                        lastMessage = "",
                                        lastTimestamp = 0L,
                                        lastSenderUid = "",
                                        lastReadByUser1 = true,
                                        lastReadByUser2 = true
                                    )

                                    chatRef.set(chatDoc) // יצירת הצאט
                                        .addOnSuccessListener { onComplete(null) }
                                        .addOnFailureListener { onComplete(it) }
                                } else {
                                    onComplete(null)
                                }
                            }
                            .addOnFailureListener { onComplete(it) }
                    }
                    .addOnFailureListener { onComplete(it) }
            }
            .addOnFailureListener { onComplete(it) }
    }

    fun listenMessages(
        chatId: String,
        onUpdate: (List<Message>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return fs.messagesCollection(chatId)
            .orderBy(Constants.Firestore.Fields.TIMESTAMP)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { docToMessageSafe(it) }
                onUpdate(list)
            }
    }

    // שולח הודעה לצאט מוסיף אותה לקולקשיין ומעדכן את השדות
    fun sendMessage(
        chatId: String,
        myUid: String,
        otherUid: String,
        text: String,
        onComplete: (Exception?) -> Unit
    ) {
        val ts = System.currentTimeMillis()
        val msg = Message(fromUid = myUid, text = text, timestamp = ts)

        val chatRef = fs.chatsCollection().document(chatId)

        fs.messagesCollection(chatId)
            .add(msg)
            .addOnSuccessListener {

                val user1Uid = if (myUid < otherUid) myUid else otherUid
                val user2Uid = if (myUid < otherUid) otherUid else myUid
                val isMeUser1 = (myUid == user1Uid)

                val updateMap = mutableMapOf<String, Any>(
                    Constants.Firestore.Fields.CHAT_ID to chatId,
                    Constants.Firestore.Fields.PARTICIPANTS to listOf(myUid, otherUid),
                    Constants.Firestore.Fields.USER1_UID to user1Uid,
                    Constants.Firestore.Fields.USER2_UID to user2Uid,
                    Constants.Firestore.Fields.LAST_MESSAGE to text,
                    Constants.Firestore.Fields.LAST_TIMESTAMP to ts,
                    Constants.Firestore.Fields.LAST_SENDER_UID to myUid
                )

                // מי שקורא עכשיו (השולח) נחשב כ"נקרא" אצלו, ואצל הצד השני "לא נקרא"
                if (isMeUser1) {
                    updateMap[Constants.Firestore.Fields.LAST_READ_BY_USER1] = true
                    updateMap[Constants.Firestore.Fields.LAST_READ_BY_USER2] = false
                } else {
                    updateMap[Constants.Firestore.Fields.LAST_READ_BY_USER1] = false
                    updateMap[Constants.Firestore.Fields.LAST_READ_BY_USER2] = true
                }

                chatRef.set(updateMap, SetOptions.merge()) // מעדכן בלי לדרוס
                    .addOnSuccessListener { onComplete(null) } // מחכה לסיום פעולה
                    .addOnFailureListener { onComplete(it) } // מחכה לשגיאה
            }
            .addOnFailureListener { onComplete(it) }
    }

    fun markChatAsRead(chatId: String, myUid: String, otherUid: String) {
        val user1Uid = if (myUid < otherUid) myUid else otherUid
        val isMeUser1 = (myUid == user1Uid)

        val updateMap = if (isMeUser1) {
            mapOf(Constants.Firestore.Fields.LAST_READ_BY_USER1 to true)
        } else {
            mapOf(Constants.Firestore.Fields.LAST_READ_BY_USER2 to true)
        }

        fs.chatsCollection().document(chatId)
            .set(updateMap, SetOptions.merge())
    }

    fun listenUserPresence(
        uid: String,
        onUpdate: (online: Boolean, lastSeen: Long) -> Unit
    ): ListenerRegistration {
        return fs.usersCollection().document(uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val online = snap.getBoolean(Constants.Firestore.Fields.ONLINE) ?: false
                val lastSeen = snap.getLong(Constants.Firestore.Fields.LAST_SEEN) ?: 0L
                onUpdate(online, lastSeen)
            }
    }

    private fun docToMessageSafe(d: DocumentSnapshot): Message? { // המרה להודעה
        return try {
            val fromUid = d.getString(Constants.Firestore.Fields.FROM_UID) ?: return null
            val text = d.getString(Constants.Firestore.Fields.TEXT) ?: ""

            val tsAny = d.get(Constants.Firestore.Fields.TIMESTAMP)
            val ts = when (tsAny) {
                is Long -> tsAny
                is Number -> tsAny.toLong()
                is Timestamp -> tsAny.toDate().time
                else -> 0L
            }

            Message(fromUid = fromUid, text = text, timestamp = ts)
        } catch (_: Exception) {
            null
        }
    }
}
