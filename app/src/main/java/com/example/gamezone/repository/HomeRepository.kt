package com.example.gamezone.repository

import com.example.gamezone.model.Post
import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.utilities.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

// שכבת הדאטה של מסך ה-Home:
//הוא מביא ומעדכן את הפיד מול Firestore, יוצר פוסטים, מנהל לייקים בצורה בטוחה, ובודק אילו פוסטים המשתמש כבר לייקק.
class HomeRepository(
    private val fs: FirestoreService = FirestoreService()
) {


    //  מאזין לפיד

    fun listenToFeed(
        onUpdate: (List<Post>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return fs.postsCollection()
            .orderBy(Constants.Firestore.Fields.CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.map { d ->
                    val p = d.toObject(Post::class.java) ?: Post()
                    p.copy(id = d.id)
                }
                onUpdate(list)
            }
    }

  // יוצר פוסט חדש וכשמעלים פוסט, הוא נשמר יחד עם הפרטים שצריך כדי להציג אותו בפיד.
    fun createPost(
        uid: String,
        text: String,
        onComplete: (Exception?) -> Unit
    ) {
        fs.usersCollection().document(uid).get()
            .addOnSuccessListener { userSnap ->
                val name = userSnap.getString(Constants.Firestore.Fields.USERNAME) ?: "Player"
                val avatar = userSnap.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"

                val data = hashMapOf(
                    Constants.Firestore.Fields.TEXT to text,
                    Constants.Firestore.Fields.AUTHOR_UID to uid,
                    Constants.Firestore.Fields.AUTHOR_NAME to name,
                    Constants.Firestore.Fields.AUTHOR_AVATAR_RES to avatar,
                    Constants.Firestore.Fields.CREATED_AT to FieldValue.serverTimestamp(),
                    Constants.Firestore.Fields.LIKE_COUNT to 0,
                    Constants.Firestore.Fields.COMMENT_COUNT to 0
                )

                fs.postsCollection().add(data)
                    .addOnSuccessListener { onComplete(null) }
                    .addOnFailureListener { e -> onComplete(e) }
            }
            .addOnFailureListener { e -> onComplete(e) }
    }

    // לייק או אנלייק עם Transaction כדי למנוע בעיות של ספירה לא נכונה
    fun toggleLike(
        postId: String,
        uid: String,
        onComplete: (isLiked: Boolean, error: Exception?) -> Unit
    ) {
        val postRef = fs.postDoc(postId)
        val likeRef = fs.postLikesCollection(postId).document(uid)

        fs.postsCollection().firestore.runTransaction { tx ->
            val likeSnap = tx.get(likeRef)
            val postSnap = tx.get(postRef)

            val currentLikes = (postSnap.getLong(Constants.Firestore.Fields.LIKE_COUNT) ?: 0L)

            if (likeSnap.exists()) {
                tx.delete(likeRef)
                tx.update(
                    postRef,
                    Constants.Firestore.Fields.LIKE_COUNT,
                    (currentLikes - 1).coerceAtLeast(0)
                )
                false
            } else {
                tx.set(
                    likeRef,
                    mapOf(Constants.Firestore.Fields.CREATED_AT to FieldValue.serverTimestamp())
                )
                tx.update(postRef, Constants.Firestore.Fields.LIKE_COUNT, currentLikes + 1)
                true
            }
        }.addOnSuccessListener { isLiked ->
            onComplete(isLiked, null)
        }.addOnFailureListener { e ->
            onComplete(false, e)
        }
    }

    // בודק לאילו פוסטים המשתמש עשה לייק ומחזיר map אם עשה לפוסט
    // זה מאפשר ל-UI לצבוע את אייקון הלייק נכון (מלא/ריק) לכל פוסט.
    fun loadMyLikes(
        uid: String,
        postIds: List<String>,
        onUpdate: (Map<String, Boolean>) -> Unit
    ) {
        if (postIds.isEmpty()) {
            onUpdate(emptyMap())
            return
        }

        val result = HashMap<String, Boolean>()
        var remaining = postIds.size

        postIds.forEach { pid ->
            fs.postLikesCollection(pid).document(uid).get()
                .addOnSuccessListener { snap ->
                    result[pid] = snap.exists()
                }
                .addOnFailureListener {
                    // אם נכשל, פשוט נניח false כדי לא לתקוע UI
                    result[pid] = false
                }
                .addOnCompleteListener {
                    remaining--
                    if (remaining == 0) onUpdate(result)
                }
        }
    }
}