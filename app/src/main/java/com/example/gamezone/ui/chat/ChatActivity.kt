package com.example.gamezone.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gamezone.R
import com.example.gamezone.databinding.ActivityChatBinding
import com.example.gamezone.model.Message
import com.example.gamezone.repository.ChatRepository
import com.example.gamezone.utilities.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.TimeUnit
import com.example.gamezone.remote.FirestorePresence

// מציג את מסך הצ’אט ומחבר אותו ל־Firestore בזמן אמת (הודעות + נוכחות), כולל שליחה וסימון נקרא
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { ChatRepository() }

    private lateinit var adapter: MessagesAdapter
    private val items = ArrayList<Message>()

    private var myUid: String = ""
    private var otherUid: String = ""
    private var otherName: String = ""
    private var chatId: String = ""

    private var presenceListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // Toolbar של הצ'אט
        setSupportActionBar(binding.chatToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.chatToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // מזהים של המשתמשים
        myUid = auth.currentUser?.uid ?: ""
        otherUid = intent.getStringExtra(Constants.Intents.EXTRA_OTHER_UID) ?: ""
        otherName = intent.getStringExtra(Constants.Intents.EXTRA_OTHER_NAME)
            ?: getString(R.string.default_username)

        // אם חסר UID כלשהו - סוגרים מסך
        if (myUid.isBlank() || otherUid.isBlank()) {
            Toast.makeText(this, getString(R.string.chat_open_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // מזהה צ'אט קבוע לפי שני UID-ים
        chatId = repo.buildChatId(myUid, otherUid)

        // כותרת + סטטוס התחלתי
        binding.chatTitle.text = otherName
        setSubtitleOffline(0L)

        setupRecycler()

        // האזנה לנוכחות של המשתמש השני
        presenceListener = repo.listenUserPresence(otherUid) { online, lastSeen ->
            if (online) setSubtitleOnline() else setSubtitleOffline(lastSeen)
        }

        // דואגים שמסמך הצ'אט קיים (אם חסר - ניצור)
        repo.ensureChatDocExists(
            myUid = myUid,
            otherUid = otherUid,
            fallbackOtherName = otherName,
            onOtherNameResolved = { resolved -> binding.chatTitle.text = resolved },
            onComplete = { /* אופציונלי */ }
        )

        // שליחת הודעה
        binding.chatBtnSend.setOnClickListener {
            val text = binding.chatEtMessage.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) return@setOnClickListener

            repo.sendMessage(
                chatId = chatId,
                myUid = myUid,
                otherUid = otherUid,
                text = text
            ) { err ->
                if (err != null) {
                    Toast.makeText(
                        this,
                        getString(R.string.chat_send_failed, err.message ?: "Unknown"),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    binding.chatEtMessage.setText("")
                    scrollToBottom()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // מתחילים האזנה להודעות כשהמסך פעיל
        listenForMessages()
    }

    override fun onStop() {
        super.onStop()
        // חשוב לעצור האזנה כדי למנוע זליגות / כפילויות
        messagesListener?.remove()
        messagesListener = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // גם נוכחות צריך לעצור כשהמסך נהרס
        presenceListener?.remove()
        presenceListener = null
    }

    //     הגדרת RecyclerView של ההודעות
    //     stackFromEnd=true כדי שהרשימה תתמקד בתחתית (הודעות חדשות)
    private fun setupRecycler() {
        adapter = MessagesAdapter(myUid)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatRecycler.adapter = adapter
    }

    // גלילה לתחתית להודעה האחרונה
    private fun scrollToBottom() {
        binding.chatRecycler.post {
            val count = adapter.itemCount
            if (count > 0) binding.chatRecycler.scrollToPosition(count - 1)
        }
    }

   // מאזין להודעות בזמן אמת
    private fun listenForMessages() {
        messagesListener?.remove()
        messagesListener = null

        messagesListener = repo.listenMessages(
            chatId = chatId,
            onUpdate = { list ->
                items.clear()
                items.addAll(list)

                adapter.submitList(items.toList()) {
                    scrollToBottom()
                }

                repo.markChatAsRead(chatId, myUid, otherUid)
            },
            onError = {}
        )
    }

   // מציג סטטוס אונליין
    private fun setSubtitleOnline() {
        binding.chatSubtitle.text = getString(R.string.chat_status_online)
        binding.chatSubtitle.setTextColor(ContextCompat.getColor(this, R.color.online_green))
    }


     // מציג סטטוס "Offline" או "Last seen ..."

    private fun setSubtitleOffline(lastSeenMillis: Long) {
        val text = if (lastSeenMillis > 0L) {
            getString(R.string.chat_last_seen, timeAgo(lastSeenMillis))
        } else {
            getString(R.string.chat_status_offline)
        }
        binding.chatSubtitle.text = text
        binding.chatSubtitle.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
    }

    // ממיר את הזמן לכמה זמן עבר
    private fun timeAgo(timeMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - timeMillis).coerceAtLeast(0L)

        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            mins < 1 -> getString(R.string.time_just_now)
            mins < 60 -> getString(R.string.time_minutes_ago, mins)
            hours < 24 -> getString(R.string.time_hours_ago, hours)
            else -> getString(R.string.time_days_ago, days)
        }
    }

    override fun onResume() {
        super.onResume()
        FirestorePresence.setOnline()
        listenForMessages()
    }

    override fun onPause() {
        super.onPause()
        messagesListener?.remove()
        messagesListener = null
        FirestorePresence.setOfflineDelayed()
    }
}