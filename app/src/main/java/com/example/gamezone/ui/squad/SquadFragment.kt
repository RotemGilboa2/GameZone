package com.example.gamezone.ui.squad

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gamezone.R
import com.example.gamezone.databinding.FragmentSquadBinding
import com.example.gamezone.model.Friend
import com.example.gamezone.model.FriendRequest
import com.example.gamezone.repository.SquadRepository
import com.example.gamezone.ui.chat.ChatActivity
import com.example.gamezone.utilities.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

// מציג את מסך החברים והבקשות, טוען חברים ובקשות דרך SquadRepository,
// מאזין לסטטוס נוכחות ולכמות הבקשות בזמן אמת, ומאפשר לפתוח צ'אט, לאשר או לדחות בקשות חברות.
class SquadFragment : Fragment() {

    private var _binding: FragmentSquadBinding? = null
    private val binding get() =
        requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { SquadRepository() }

    private enum class Tab { FRIENDS, REQUESTS }
    private var currentTab: Tab = Tab.FRIENDS

    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var requestsAdapter: RequestsAdapter

    private val friends = ArrayList<Friend>()
    private val requests = ArrayList<FriendRequest>()

    private var requestsListener: ListenerRegistration? = null
    private val presenceListeners = hashMapOf<String, ListenerRegistration>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSquadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupTabs()
        listenToRequestsBadge()

        switchTab(Tab.FRIENDS)
        loadFriends()
    }

    // הגדרת RecyclerView + יצירת adapters
    private fun setupRecycler() {
        friendsAdapter = FriendsAdapter { friend ->
            // פתיחת צאט
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra(Constants.Intents.EXTRA_OTHER_UID, friend.uid)
            intent.putExtra(Constants.Intents.EXTRA_OTHER_NAME, friend.username)
            startActivity(intent)
        }

        requestsAdapter = RequestsAdapter(
            onAccept = { req -> acceptRequest(req) },
            onDecline = { req -> declineRequest(req) }
        )

        binding.squadRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.squadRecycler.adapter = friendsAdapter
    }

    private fun setupTabs() {
        binding.squadBtnFriends.setOnClickListener {
            switchTab(Tab.FRIENDS)
            loadFriends()
        }

        binding.squadBtnRequests.setOnClickListener {
            switchTab(Tab.REQUESTS)
            loadRequests()
        }
    }

    private fun switchTab(tab: Tab) {
        currentTab = tab

        val selected = requireContext().getColor(R.color.nav_selected)
        val unselected = requireContext().getColor(R.color.card_inner)

        if (tab == Tab.FRIENDS) {
            binding.squadBtnFriends.setBackgroundColor(selected)
            binding.squadBtnRequests.setBackgroundColor(unselected)
            binding.squadRecycler.adapter = friendsAdapter
            binding.squadLblEmpty.text = getString(R.string.squad_empty_friends)
        } else {
            binding.squadBtnFriends.setBackgroundColor(unselected)
            binding.squadBtnRequests.setBackgroundColor(selected)
            binding.squadRecycler.adapter = requestsAdapter
            binding.squadLblEmpty.text = getString(R.string.squad_empty_requests)
        }

        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = if (currentTab == Tab.FRIENDS) friends.isEmpty() else requests.isEmpty()
        binding.squadLblEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.squadRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun loadFriends() {
        val myUid = auth.currentUser?.uid ?: return

        repo.loadFriends(myUid) { list, err ->
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.squad_failed_loading_friends,
                    err.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                return@loadFriends
            }

            friends.clear()
            friends.addAll(list)

            friendsAdapter.submitList(friends.toList())
            startFriendsPresenceListeners(friends.toList())
            updateEmptyState()

            repo.syncFriendsUsersData(myUid, friends.toList())
        }
    }

    // מאזין לנוכחות חברים שחברים שלי בלבד
    private fun startFriendsPresenceListeners(friendsList: List<Friend>) {
        val currentUids = friendsList.map { it.uid }.toSet()
        val toRemove = presenceListeners.keys.filter { it !in currentUids }
        toRemove.forEach { uid ->
            presenceListeners[uid]?.remove()
            presenceListeners.remove(uid)
        }

        friendsList.forEach { f ->
            if (f.uid.isBlank()) return@forEach
            if (presenceListeners.containsKey(f.uid)) return@forEach

            val reg = repo.listenFriendPresence(f.uid) { online, lastSeen, avatarRes, username ->
                val updated = f.copy(
                    online = online,
                    lastSeen = lastSeen,
                    avatarRes = avatarRes ?: f.avatarRes,
                    username = username ?: f.username
                )
                friendsAdapter.updateFriend(updated)
            }

            presenceListeners[f.uid] = reg
        }
    }

    private fun loadRequests() {
        val uid = auth.currentUser?.uid ?: return

        repo.loadRequests(uid) { list, err ->
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.squad_failed_loading_requests,
                    err.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                return@loadRequests
            }

            requests.clear()
            requests.addAll(list)

            requestsAdapter.submitList(requests.toList())
            updateEmptyState()
        }
    }

    private fun acceptRequest(req: FriendRequest) {
        val myUid = auth.currentUser?.uid ?: return

        repo.acceptRequestFull(myUid, req) { err ->
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.squad_accept_failed,
                    err.message ?: "Unknown"), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.squad_friend_added),
                    Toast.LENGTH_SHORT).show()
                loadRequests()
                loadFriends()
            }
        }
    }

    private fun declineRequest(req: FriendRequest) {
        val myUid = auth.currentUser?.uid ?: return

        repo.declineRequestFull(myUid, req) { err ->
            if (err != null) {
                Toast.makeText(requireContext(), getString(R.string.squad_decline_failed,
                    err.message ?: "Unknown"), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), getString(R.string.squad_request_declined),
                    Toast.LENGTH_SHORT).show()
                loadRequests()
            }
        }
    }

    // האנה בזמן אמת לכמות בקשות נכנסות
    private fun listenToRequestsBadge() {
        val uid = auth.currentUser?.uid ?: return

        requestsListener?.remove()
        requestsListener = repo.listenRequestsBadge(
            myUid = uid,
            onUpdate = { count -> updateRequestsBadge(count) },
            onError = {  }
        )
    }

    private fun updateRequestsBadge(count: Int) {
        if (count <= 0) {
            binding.squadBadgeRequests.visibility = View.GONE
        } else {
            binding.squadBadgeRequests.visibility = View.VISIBLE
            binding.squadBadgeRequests.text = if (count > 99) "99+" else count.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestsListener?.remove()
        requestsListener = null
        presenceListeners.values.forEach { it.remove() }
        presenceListeners.clear()
        _binding = null
    }
}