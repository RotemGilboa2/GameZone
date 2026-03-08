package com.example.gamezone.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gamezone.R
import com.example.gamezone.databinding.FragmentHomeBinding
import com.example.gamezone.model.Post
import com.example.gamezone.repository.HomeRepository
import com.example.gamezone.utilities.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

// מציג את מסך הפיד הראשי, מאזין לפוסטים בזמן אמת דרך HomeRepository,
// ומאפשר ליצור פוסטים, לבצע לייקים ולפתוח את מסך התגובות
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() =
        requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { HomeRepository() }

    private lateinit var postsAdapter: PostsAdapter
    private var postsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postsAdapter = PostsAdapter(
            onLike = { post -> toggleLike(post) },
            onComments = { post -> openComments(post) }
        )

        binding.homeRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecycler.adapter = postsAdapter

        binding.homeBtnNewPost.setOnClickListener { showCreatePostDialog() }

        listenToFeed()
    }

    private fun listenToFeed() {
        postsListener?.remove()
        postsListener = repo.listenToFeed(
            onUpdate = { list ->
                postsAdapter.submitList(list)
                loadMyLikesForVisiblePosts(list.map { it.id })
            },
            onError = {
            }
        )
    }


     // דיאלוג ליצירת פוסט חדש
    private fun showCreatePostDialog() {
        val input = EditText(requireContext())
        input.hint = getString(R.string.home_new_post_hint)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.home_new_post_title))
            .setView(input)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_post)) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) createPost(text)
            }
            .show()
    }


     // יצירת פוסט דרך הריפו.
    private fun createPost(text: String) {
        val uid = auth.currentUser?.uid ?: return

        repo.createPost(uid = uid, text = text) { e ->
            if (e != null) {
                Toast.makeText(requireContext(), getString(R.string.home_failed_to_post), Toast.LENGTH_SHORT).show()
            }
        }
    }

     // לייק/אנלייק דרך הריפו + עדכון UI של האדפטר.
    private fun toggleLike(post: Post) {
        val uid = auth.currentUser?.uid ?: return

        repo.toggleLike(postId = post.id, uid = uid) { isLiked, err ->
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.home_like_failed), Toast.LENGTH_SHORT).show()
                return@toggleLike
            }
            postsAdapter.setLiked(post.id, isLiked)
        }
    }


    private fun openComments(post: Post) {
        val intent = Intent(requireContext(), CommentsActivity::class.java)
        intent.putExtra(Constants.Intents.EXTRA_POST_ID, post.id)
        startActivity(intent)
    }

    // טוען עבור הפוסטים שמופיעים כרגע אם עשיתי עליהם לייק,
    // ומעדכן את האדפטר בהתאם.
    private fun loadMyLikesForVisiblePosts(postIds: List<String>) {
        val uid = auth.currentUser?.uid ?: return

        repo.loadMyLikes(uid = uid, postIds = postIds) { map ->
            map.forEach { (pid, liked) ->
                postsAdapter.setLiked(pid, liked)
            }
        }
    }

    // למנוע דליפת זכרון והאזנות כפולות
    override fun onDestroyView() {
        super.onDestroyView()
        postsListener?.remove()
        postsListener = null
        _binding = null
    }
}