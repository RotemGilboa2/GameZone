package com.example.gamezone.repository

import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.utilities.Constants

// הפונקציה מעדכנת את האווטאר של המשתמש ב-Firestore דרך שכבת Repository, שפונה ל-FirestoreService כדי לגשת למסד הנתונים
class AvatarRepository(
    private val fs: FirestoreService = FirestoreService()
) {
    fun saveAvatar(uid: String, avatarRes: String, onComplete: (Exception?) -> Unit) {
        fs.usersCollection().document(uid)
            .update(mapOf(Constants.Firestore.Fields.AVATAR_RES to avatarRes))
            .addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { e -> onComplete(e) }
    }
}