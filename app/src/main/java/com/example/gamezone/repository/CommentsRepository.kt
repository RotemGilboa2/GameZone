package com.example.gamezone.repository

import com.example.gamezone.model.Comment
import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.utilities.Constants
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

// מטפל בכל מה שקשור לתגובות על פוסטים כמו האזנה לרשימת תגובות בזמן אמת והוספת תגובה חדשה ועדכון מס' התגובות (batch)
class CommentsRepository(
    private val fs: FirestoreService = FirestoreService()
) {

    fun listenComments(
        postId: String,
        onUpdate: (List<Comment>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return fs.postCommentsCollection(postId)
            .orderBy(Constants.Firestore.Fields.CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener onError(err)
                if (snap == null) return@addSnapshotListener

                val list = snap.documents.map { d ->
                    val c = d.toObject(Comment::class.java) ?: Comment()
                    c.copy(id = d.id)
                }
                onUpdate(list)
            }
    }

    fun sendComment(
        postId: String,
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
                    Constants.Firestore.Fields.CREATED_AT to FieldValue.serverTimestamp()
                )

                val db = fs.postsCollection().firestore
                val postRef = fs.postDoc(postId)
                val commentRef = fs.postCommentsCollection(postId).document()

                val batch = db.batch()
                batch.set(commentRef, data)
                batch.update(postRef, Constants.Firestore.Fields.COMMENT_COUNT,
                    FieldValue.increment(1))

                batch.commit()
                    .addOnSuccessListener { onComplete(null) }
                    .addOnFailureListener { e -> onComplete(e) }
            }
            .addOnFailureListener { e -> onComplete(e) }
    }
}