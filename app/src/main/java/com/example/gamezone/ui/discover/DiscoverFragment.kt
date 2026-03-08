package com.example.gamezone.ui.discover

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gamezone.R
import com.example.gamezone.databinding.FragmentDiscoverBinding
import com.example.gamezone.model.User
import com.example.gamezone.repository.DiscoverRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.example.gamezone.utilities.Constants


// מציג את מסך Discover, מחפש ומסנן משתמשים דרך ה־Repository,
// מחריג משתמשים לא רלוונטיים, ומאפשר לשלוח בקשות חברות מתוך הרשימה.
class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null

    private val binding get() = requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val currentUid get() = auth.currentUser?.uid

    private val repo by lazy { DiscoverRepository() }
    private lateinit var adapter: DiscoverAdapter

    // exclusions מה לא להציג ברשימה
    private val excludedUids = hashSetOf<String>()

    // listeners
    private var friendsListener: ListenerRegistration? = null
    private var incomingListener: ListenerRegistration? = null
    private var sentListener: ListenerRegistration? = null

    // snapshots cache
    private var friendsSnap: QuerySnapshot? = null
    private var incomingSnap: QuerySnapshot? = null
    private var sentSnap: QuerySnapshot? = null

    // game filter
    private var selectedGame: String? = null
    private val allGames by lazy { resources.getStringArray(R.array.games_catalog).toList() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupGameFilter()

        startExcludedRealtime()
        searchUsers("") // initial
    }

    private fun setupRecyclerView() {
        adapter = DiscoverAdapter { userToAdd ->
            sendFriendRequest(userToAdd)
        }

        binding.discoverRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.discoverRecycler.adapter = adapter
    }

    private fun setupSearch() {
        binding.discoverEtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchUsers(s?.toString()?.trim().orEmpty())
            }
        })
    }

    private fun setupGameFilter() {
        val arr = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, allGames)
        binding.discoverEtGame.setAdapter(arr)

        binding.discoverEtGame.setOnItemClickListener { parent, _, position, _ ->
            selectedGame = parent.getItemAtPosition(position) as String
            updateClearButtonVisibility()
            searchUsers(binding.discoverEtSearch.text?.toString()?.trim().orEmpty())
        }

        binding.discoverBtnClearGame.setOnClickListener {
            selectedGame = null
            binding.discoverEtGame.setText("", false)
            updateClearButtonVisibility()
            searchUsers(binding.discoverEtSearch.text?.toString()?.trim().orEmpty())
        }

        updateClearButtonVisibility()
    }

    private fun updateClearButtonVisibility() {
        binding.discoverBtnClearGame.visibility =
            if (selectedGame.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    private fun startExcludedRealtime() {
        val uid = currentUid ?: return

        excludedUids.clear()
        excludedUids.add(uid)

        friendsSnap = null
        incomingSnap = null
        sentSnap = null

        friendsListener?.remove()
        incomingListener?.remove()
        sentListener?.remove()

        friendsListener = repo.listenFriends(
            myUid = uid,
            onUpdate = { snap ->
                friendsSnap = snap
                rebuildExcludedFromSnaps(uid)
            },
            onError = { }
        )

        incomingListener = repo.listenIncoming(
            myUid = uid,
            onUpdate = { snap ->
                incomingSnap = snap
                rebuildExcludedFromSnaps(uid)
            },
            onError = { }
        )

        sentListener = repo.listenSent(
            myUid = uid,
            onUpdate = { snap ->
                sentSnap = snap
                rebuildExcludedFromSnaps(uid)
            },
            onError = { }
        )

        rebuildExcludedFromSnaps(uid)
    }

    private fun rebuildExcludedFromSnaps(uid: String) {
        val newExcluded = hashSetOf(uid)

        friendsSnap?.documents?.forEach { doc -> newExcluded.add(doc.id) }
        incomingSnap?.documents?.forEach { doc -> newExcluded.add(doc.id) }
        sentSnap?.documents?.forEach { doc -> newExcluded.add(doc.id) }

        excludedUids.clear()
        excludedUids.addAll(newExcluded)

        searchUsers(binding.discoverEtSearch.text?.toString()?.trim().orEmpty())
    }

    private fun searchUsers(queryText: String) {
        repo.searchUsers(
            queryText = queryText,
            selectedGame = selectedGame,
            excludedUids = excludedUids,
            onComplete = { users, err ->
                if (err != null) {
                    Toast.makeText(requireContext(), "Error searching users", Toast.LENGTH_SHORT).show()
                    return@searchUsers
                }

                adapter.updateList(users)
                binding.discoverLblFoundCount.text = "${users.size} found"
            }
        )
    }

    private fun sendFriendRequest(userToAdd: User) {
        val myUid = currentUid ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        repo.sendFriendRequest(myUid, userToAdd) { e ->
            if (e == null) {
                Toast.makeText(requireContext(), "Request sent to ${userToAdd.username}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Request failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        friendsListener?.remove()
        incomingListener?.remove()
        sentListener?.remove()

        friendsListener = null
        incomingListener = null
        sentListener = null

        friendsSnap = null
        incomingSnap = null
        sentSnap = null

        _binding = null
    }
}