package com.example.gamezone.ui.chat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gamezone.model.Chat
import com.example.gamezone.remote.FirestoreService
import com.example.gamezone.databinding.FragmentChatListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.example.gamezone.R
import com.example.gamezone.utilities.Constants

// מציג את רשימת הצ’אטים של המשתמש בזמן אמת, מאפשר חיפוש לפי שם,
// מסנכרן שמות ואווטארים מעודכנים של המשתמשים, ופותח את מסך השיחה המתאים בעת לחיצה על צ’אט.
class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }


    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirestoreService() }

    private lateinit var adapter: ChatListAdapter
    private val allChats = ArrayList<Chat>()
    private val filteredChats = ArrayList<Chat>()

    private var myUid: String = ""

    private var chatsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myUid = auth.currentUser?.uid.orEmpty()
        if (myUid.isBlank()) {
            context?.let {
                Toast.makeText(
                    it, getString(R.string.error_not_logged_in),

                    Toast.LENGTH_SHORT
                ).show() }
            return
        }

        setupRecycler()
        setupSearch()
        listenForChats()
    }

    override fun onStop() {
        super.onStop()
        // כדי שלא ימשיך לקבל snapshots כשאת בתוך ChatActivity
        chatsListener?.remove()
        chatsListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //  ביטחון כפול: אם onStop לא קרה מסיבה כלשהי
        chatsListener?.remove()
        chatsListener = null
        _binding = null
    }

    private fun setupRecycler() {
        adapter = ChatListAdapter(myUid) { otherUid, otherName ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra(Constants.Intents.EXTRA_OTHER_UID, otherUid)
            intent.putExtra(Constants.Intents.EXTRA_OTHER_NAME, otherName)
            startActivity(intent)
        }

        binding.chatListRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecycler.adapter = adapter
    }

    private fun setupSearch() {
        binding.chatListEtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChats(s?.toString().orEmpty())
            }
        })
    }

    private fun listenForChats() {
        //  להסיר מאזין קודם כדי למנוע כפולים
        chatsListener?.remove()
        chatsListener = null

        chatsListener = firestore.chatsCollection()
            .whereArrayContains(Constants.Firestore.Fields.PARTICIPANTS, myUid)
            .addSnapshotListener { snap, err ->

                // אם ה-view נהרס או ה-fragment לא מחובר, לא עושים כלום
                val b = _binding ?: return@addSnapshotListener
                val ctx = context ?: return@addSnapshotListener

                if (err != null) {
                    Toast.makeText(ctx, getString(R.string.error_loading_chats)
                            + ": ${err.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener

                allChats.clear()
                allChats.addAll(snap.toObjects(Chat::class.java))

                syncChatsUsersData(allChats)

                allChats.sortByDescending { it.lastTimestamp }

                val q = b.chatListEtSearch.text?.toString().orEmpty()
                filterChats(q)
            }
    }

    // מעדכן עם merge כדי שתמיד יהיה שם ואווטר עדכני
    private fun syncChatsUsersData(chats: List<Chat>) {
        // אם ה-view לא קיים/fragment לא מחובר – לא מריצים
        if (_binding == null || !isAdded) return

        val otherUids = chats.mapNotNull { c ->
            val otherUid = if (c.user1Uid == myUid) c.user2Uid else c.user1Uid
            otherUid.takeIf { it.isNotBlank() }
        }.distinct()

        for (uid in otherUids) {
            firestore.usersCollection().document(uid).get()
                .addOnSuccessListener { userSnap ->
                    // אם בינתיים ה-view נהרס – לא נוגעים
                    if (_binding == null || !isAdded) return@addOnSuccessListener

                    val freshName = userSnap.getString(Constants.Firestore.Fields.USERNAME) ?: "Player"
                    val freshAvatar = userSnap.getString(Constants.Firestore.Fields.AVATAR_RES) ?: "logo"

                    for (c in chats) {
                        val isMeUser1 = (c.user1Uid == myUid)
                        val thisOtherUid = if (isMeUser1) c.user2Uid else c.user1Uid
                        if (thisOtherUid != uid) continue

                        val update: Map<String, Any>

                        if (myUid < uid) {
                            // user1=myUid user2=uid -> נעדכן user2
                            val alreadySame =
                                (c.user2Name == freshName) && (c.user2Avatar == freshAvatar)
                            if (alreadySame) continue

                            update = mapOf(
                                Constants.Firestore.Fields.USER2_NAME to freshName,
                                Constants.Firestore.Fields.USER2_AVATAR to freshAvatar
                            )
                        } else {
                            // user1=uid user2=myUid -> נעדכן user1
                            val alreadySame =
                                (c.user1Name == freshName) && (c.user1Avatar == freshAvatar)
                            if (alreadySame) continue

                            update = mapOf(
                                Constants.Firestore.Fields.USER1_NAME to freshName,
                                Constants.Firestore.Fields.USER1_AVATAR to freshAvatar
                            )
                        }

                        firestore.chatsCollection().document(c.chatId)
                            .set(update, SetOptions.merge())
                    }
                }
        }
    }

    private fun filterChats(query: String) {
        val q = query.trim().lowercase()

        filteredChats.clear()
        if (q.isBlank()) {
            filteredChats.addAll(allChats)
        } else {
            for (c in allChats) {
                val otherName = if (c.user1Uid == myUid) c.user2Name else c.user1Name
                if (otherName.lowercase().contains(q)) {
                    filteredChats.add(c)
                }
            }
        }

        adapter.submitList(filteredChats.toList())
    }
}

