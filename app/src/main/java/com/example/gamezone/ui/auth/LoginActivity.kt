package com.example.gamezone.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gamezone.MainActivity
import com.example.gamezone.R
import com.example.gamezone.model.User
import com.example.gamezone.repository.UserRepository
import com.example.gamezone.ui.profile.ProfileSetupActivity
import com.example.gamezone.utilities.Constants
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

//מטפל בהתחברות + יצירת משתמש ב-Firestore אם צריך + החלטה לאן לשלוח את המשתמש (פיד או השלמת פרופיל)
class LoginActivity : AppCompatActivity() {

    private val userRepo = UserRepository()

    private fun signIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.drawable.logo)
            .setAvailableProviders(providers)
            .setTheme(R.style.Theme_GameZone)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        onSignInResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // התאמת padding לפי system bars (status/navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // אם אין משתמש מחובר מתחילים התחברות
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            signIn()
        } else {
            // כבר מחובר דואגים שיש משתמש בפיירסטור ואז מנתבים
            createFirestoreUserIfMissingThenRoute(isReturningUser = true)
        }
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) {
            createFirestoreUserIfMissingThenRoute(isReturningUser = false)
        } else {
            Toast.makeText(this, getString(R.string.login_error_failed), Toast.LENGTH_LONG).show()
            signIn()
        }
    }

// יוצר משתמש אם לא קיים ואז הולך להשלמת הפרופיל
    private fun createFirestoreUserIfMissingThenRoute(isReturningUser: Boolean) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            Toast.makeText(this, getString(R.string.login_failed_no_user), Toast.LENGTH_SHORT).show()
            signIn()
            return
        }

        val uid = firebaseUser.uid
        val usernameFromAuth = firebaseUser.displayName ?: ""

        val newUser = User(
            uid = uid,
            username = usernameFromAuth,
            avatarRes = Constants.Defaults.DEFAULT_AVATAR,
            createdAt = System.currentTimeMillis(),
            profileCompleted = false
        )

        userRepo.createUserIfMissing(newUser) { _, error ->
            if (error != null) {
                Toast.makeText(
                    this,
                    getString(R.string.login_firestore_error, error.message ?: "Unknown"),
                    Toast.LENGTH_LONG
                ).show()
                return@createUserIfMissing
            }

            // טוענים את המשתמש מהפיירסטור כדי לדעת אם הפרופיל הושלם
            userRepo.getUser(uid) { userDoc, err ->
                if (err != null || userDoc == null) {
                    Toast.makeText(this, getString(R.string.login_failed_load_profile), Toast.LENGTH_LONG).show()
                    return@getUser
                }

                val name = userDoc.username.ifBlank { getString(R.string.default_username) }

                // הודעת ברכה גם בחזרה וגם בהתחברות חדשה
                if (isReturningUser) {
                    Toast.makeText(this, getString(R.string.login_welcome_back, name), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, getString(R.string.login_welcome_new, name), Toast.LENGTH_SHORT).show()
                }

                if (userDoc.profileCompleted) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                }
                finish()
            }
        }
    }
}