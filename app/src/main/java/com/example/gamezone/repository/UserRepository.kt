package com.example.gamezone.repository

import com.example.gamezone.model.User
import com.example.gamezone.remote.FirestoreService
import com.google.firebase.firestore.SetOptions

// מנהל את הדאטה של משתמשים מול Firestore: יצירת משתמש אם אינו קיים,
// עדכון שדות במסמך המשתמש מבלי לדרוס נתונים קיימים, ושליפת פרטי משתמש מהמסד לפי ה-UID.
class UserRepository(
    private val fs: FirestoreService = FirestoreService()
) {

    fun createUserIfMissing(
        user: User,
        onComplete: (createdNow: Boolean, error: Exception?) -> Unit
    ) {
        val docRef = fs.usersCollection().document(user.uid)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onComplete(false, null)
                } else {
                    docRef.set(user)
                        .addOnSuccessListener { onComplete(true, null) }
                        .addOnFailureListener { e -> onComplete(false, e) }
                }
            }
            .addOnFailureListener { e -> onComplete(false, e) }
    }

    fun updateUser(
        uid: String,
        updates: Map<String, Any>,
        onComplete: (Exception?) -> Unit
    ) {
        fs.usersCollection()
            .document(uid)
            .set(updates, SetOptions.merge())
            .addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { e -> onComplete(e) }
    }

     // מחזיר User עם uid נכון מה־docId
    fun getUser(
        uid: String,
        onComplete: (user: User?, error: Exception?) -> Unit
    ) {
        fs.usersCollection().document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val user = snap.toObject(User::class.java)?.copy(uid = uid)
                onComplete(user, null)
            }
            .addOnFailureListener { e -> onComplete(null, e) }
    }
}