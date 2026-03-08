package com.example.gamezone.ui.home

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gamezone.databinding.ActivityCommentsBinding
import com.example.gamezone.repository.CommentsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.example.gamezone.R
import com.example.gamezone.utilities.Constants
import com.example.gamezone.remote.FirestorePresence

// מסך התגובות של פוסט באפליקציה
//התפקיד שלו הוא להציג את כל התגובות לפוסט מסוים בזמן אמת ולאפשר למשתמש להוסיף תגובה חדשה
class CommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { CommentsRepository() }

    private lateinit var adapter: CommentsAdapter
    private var commentsListener: ListenerRegistration? = null

    private var postId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.commentsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.comments_title)
        binding.commentsToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        postId = intent.getStringExtra(Constants.Intents.EXTRA_POST_ID) ?: ""
        if (postId.isBlank()) { finish(); return }

        adapter = CommentsAdapter()
        binding.commentsRecycler.layoutManager = LinearLayoutManager(this)
        binding.commentsRecycler.adapter = adapter
        // שליחה
        binding.commentsBtnSend.setOnClickListener {
            val text = binding.commentsEdt.text.toString().trim()
            if (text.isNotEmpty()) sendComment(text)
        }

        listenToComments()
    }

    private fun listenToComments() {
        commentsListener?.remove()
        commentsListener = repo.listenComments(
            postId = postId,
            onUpdate = { list ->
                adapter.submitList(list)
                if (list.isNotEmpty()) binding.commentsRecycler.scrollToPosition(0)
            },
            onError = {
            }
        )
    }

    private fun sendComment(text: String) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, getString(R.string.error_not_logged_in),
                Toast.LENGTH_SHORT).show()
            return
        }

        repo.sendComment(postId = postId, uid = uid, text = text) { e ->
            if (e == null) {
                binding.commentsEdt.setText("")
            } else {
                Toast.makeText(this, getString(R.string.comments_failed_to_comment,
                    e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        commentsListener?.remove()
        commentsListener = null
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
