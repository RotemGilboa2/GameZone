package com.example.gamezone.remote

import android.os.Handler
import android.os.Looper
import com.example.gamezone.utilities.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestorePresence { // מעדכן סטטוס נוכחות לשרת

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val fs by lazy { FirebaseFirestore.getInstance() }

    // handler לדחיית offline (מונע קפיצה בין מסכים)
    private val handler = Handler(Looper.getMainLooper())
    private var offlineRunnable: Runnable? = null

    private fun setPresence(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        fs.collection(Constants.Firestore.USERS)
            .document(uid)
            .set(
                mapOf(
                    Constants.Firestore.Fields.ONLINE to online,
                    Constants.Firestore.Fields.LAST_SEEN to System.currentTimeMillis()
                ),
                SetOptions.merge() // לא לדרוס שדות אחרים במסמך
            )
    }

    // מסמן Online מיד + מבטל Offline שתוזמן
    fun setOnline() {
        offlineRunnable?.let { handler.removeCallbacks(it) }
        offlineRunnable = null
        setPresence(true)
    }


     // מונע Online→Offline→Online כשעוברים בין מסכים
    fun setOfflineDelayed(delayMs: Long = 1000L) {
        offlineRunnable?.let { handler.removeCallbacks(it) }

        val r = Runnable {
            setPresence(false)
        }

        offlineRunnable = r
        handler.postDelayed(r, delayMs)
    }

    // אם באמת צריך Offline מיידי (logout למשל)
    fun setOfflineNow() {
        offlineRunnable?.let { handler.removeCallbacks(it) }
        offlineRunnable = null
        setPresence(false)
    }
}