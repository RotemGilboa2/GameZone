package com.example.gamezone.remote

import com.example.gamezone.utilities.Constants
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreService {

    // גישה לDB
    // Fragments / Activities → אחראים על UI
    // Repositories → אחראים על דאטה
    // FirestoreService אחראי רק על גישה למסד הנתונים
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


    // מחזיר רפרנס לקולקשן users

    fun usersCollection(): CollectionReference =
        db.collection(Constants.Firestore.USERS)

    fun friendRequestsCollection(uid: String): CollectionReference =
        usersCollection()
            .document(uid)
            .collection(Constants.Firestore.FRIEND_REQUESTS)

    fun friendsCollection(uid: String): CollectionReference =
        usersCollection()
            .document(uid)
            .collection(Constants.Firestore.FRIENDS)

    fun sentRequestsCollection(uid: String): CollectionReference =
        usersCollection()
            .document(uid)
            .collection(Constants.Firestore.SENT_REQUESTS)


    fun chatsCollection(): CollectionReference =
        db.collection(Constants.Firestore.CHATS)

    fun messagesCollection(chatId: String): CollectionReference =
        chatsCollection()
            .document(chatId)
            .collection(Constants.Firestore.MESSAGES)

    fun postsCollection(): CollectionReference =
        db.collection(Constants.Firestore.POSTS)

    fun postDoc(postId: String): DocumentReference =
        postsCollection().document(postId)

    fun postLikesCollection(postId: String): CollectionReference =
        postDoc(postId).collection(Constants.Firestore.LIKES)


    fun postCommentsCollection(postId: String): CollectionReference =
        postDoc(postId).collection(Constants.Firestore.COMMENTS)
}

