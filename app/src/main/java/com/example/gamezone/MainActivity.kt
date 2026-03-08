package com.example.gamezone

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.gamezone.databinding.ActivityMainBinding
import androidx.core.view.updatePadding
import com.example.gamezone.remote.FirestorePresence



// המסך הראשי של האפליקציה שמכיל את כל הפרגמנטים, מחבר את ה-Bottom Navigation
// לניווט ביניהם, ומעדכן את סטטוס הנוכחות של המשתמש ב-Firestore
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // נותנים Padding רק למעלה למסך התוכן
            binding.mainNavHost.updatePadding(top = systemBars.top)

            // נותנים Padding רק למטה לבר התחתון (כדי שלא ייכנס מתחת לניווט של המכשיר)
            binding.mainBottomNav.updatePadding(bottom = systemBars.bottom)

            insets
        }


        val navHost = supportFragmentManager.findFragmentById(R.id.main_nav_host) as NavHostFragment
        binding.mainBottomNav.setupWithNavController(navHost.navController)
    }

    override fun onResume() {
        super.onResume()
        FirestorePresence.setOnline()
    }

    override fun onPause() {
        super.onPause()
        FirestorePresence.setOfflineDelayed()
    }

}

