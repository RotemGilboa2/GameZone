package com.example.gamezone.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gamezone.MainActivity
import com.example.gamezone.R
import com.example.gamezone.repository.UserRepository
import com.example.gamezone.databinding.ActivityProfileSetupBinding
import com.example.gamezone.model.GameItem
import com.example.gamezone.utilities.Constants
import com.google.firebase.auth.FirebaseAuth

// מציג את מסך השלמת הפרופיל הראשוני, מאפשר למשתמש להזין שם משתמש ולבחור משחקים מועדפים,
// שומר את הנתונים דרך UserRepository, ולאחר מכן מעביר למסך הראשי של האפליקציה.
class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private val userRepo = UserRepository()

    private lateinit var gamesAdapter: GamesAdapter
    private val maxGames = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // יוצרת אובייקט ViewBinding שמחבר בין הקוד ל־XML ומאפשר גישה ישירה לכל הרכיבים במסך
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupGames()
        setupSave()
    }

    private fun setupGames() {
        val games = resources.getStringArray(R.array.games_catalog)
            .map { GameItem(name = it, isSelected = false) }

        gamesAdapter = GamesAdapter(
            items = games,
            maxSelection = maxGames,
            onLimitReached = {
                Toast.makeText(this,  getString(R.string.profile_setup_limit_games,
                    maxGames), Toast.LENGTH_SHORT).show()
            }
        )

        binding.profileRVGames.layoutManager = GridLayoutManager(this, 2)
        binding.profileRVGames.adapter = gamesAdapter
    }

    private fun setupSave() {
        binding.profileBTNSave.setOnClickListener {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                Toast.makeText(this, getString(R.string.error_not_logged_in),
                    Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val username = binding.profileEDTUsername.text?.toString()?.trim().orEmpty()
            if (username.isBlank()) {
                binding.profileTILUsername.error = getString(R.string.error_not_logged_in)
                return@setOnClickListener
            }
            binding.profileTILUsername.error = null

            val selectedGames = gamesAdapter.getSelectedGameNames()
            if (selectedGames.isEmpty()) {
                Toast.makeText(this, getString(R.string.profile_setup_select_one_game),
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // עדכונים למסמך המשתמש
            val updates = hashMapOf<String, Any>(
                Constants.Firestore.Fields.USERNAME to username,
                Constants.Firestore.Fields.FAVORITE_GAMES to selectedGames,
                Constants.Firestore.Fields.PROFILE_COMPLETED to true
            )

            userRepo.updateUser(firebaseUser.uid, updates) { err ->
                if (err != null) {
                    Toast.makeText(this, getString(R.string.error_save_failed,
                        err.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                    return@updateUser
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
