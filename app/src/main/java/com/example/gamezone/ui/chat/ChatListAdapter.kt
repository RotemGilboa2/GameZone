package com.example.gamezone.ui.chat

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.R
import com.example.gamezone.databinding.ItemChatRowBinding
import com.example.gamezone.model.Chat
import com.example.gamezone.utilities.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// אחראי להציג את רשימת הצ’אטים ב-RecyclerView על ידי קישור אובייקטי Chat
// לשורות שמציגות את שם המשתמש השני, האווטאר, ההודעה האחרונה, זמן השליחה והאם ההודעה נקראה.
class ChatListAdapter(
    private val myUid: String,
    private val onChatClick: (otherUid: String, otherName: String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatVH>() {

    private val items = ArrayList<Chat>()

    fun submitList(newList: List<Chat>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
        val b = ItemChatRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatVH(b)
    }

    override fun onBindViewHolder(holder: ChatVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ChatVH(private val b: ItemChatRowBinding) : RecyclerView.ViewHolder(b.root) {


         // מחשבת מי המשתמש השני בצ’אט וunread
        fun bind(chat: Chat) {
            val isMeUser1 = chat.user1Uid == myUid

            val otherUid = if (isMeUser1) chat.user2Uid else chat.user1Uid
            val otherName = if (isMeUser1) chat.user2Name else chat.user1Name
            val otherAvatar = if (isMeUser1) chat.user2Avatar else chat.user1Avatar

            b.itemChatName.text = otherName
            b.itemChatLast.text = chat.lastMessage.ifBlank { "" }
            b.itemChatTime.text = formatTime(chat.lastTimestamp)

            // טעינת אווטאר לפי שם drawable (string) שמגיע מה-DB
            val ctx = b.root.context
            val resId = ctx.resources.getIdentifier(otherAvatar, Constants.Resources.DRAWABLE, ctx.packageName)
            b.itemChatAvatar.setImageResource(if (resId != 0) resId else R.drawable.logo)

            // unread רק אם:
            // יש הודעה בכלל + ההודעה האחרונה מהצד השני + הדגל שלי "לא נקרא"
            val myReadFlag = if (isMeUser1) chat.lastReadByUser1 else chat.lastReadByUser2
            val isUnread = chat.lastTimestamp != 0L && chat.lastSenderUid != myUid && !myReadFlag

            if (isUnread) {
                // הדגשה להודעה שלא נקראה
                b.itemChatLast.setTypeface(null, Typeface.BOLD)
                b.itemChatLast.setTextColor(ctx.getColor(R.color.text_primary))
                b.itemChatName.setTypeface(null, Typeface.BOLD)
            } else {
                // מצב רגיל
                b.itemChatLast.setTypeface(null, Typeface.NORMAL)
                b.itemChatLast.setTextColor(ctx.getColor(R.color.text_muted))
                b.itemChatName.setTypeface(null, Typeface.NORMAL)
            }

            // קליק על שורה פתיחת הצ'אט עם הצד השני
            b.root.setOnClickListener {
                onChatClick(otherUid, otherName)
            }
        }

        private fun formatTime(ts: Long): String {
            if (ts == 0L) return ""
            val sdf = SimpleDateFormat(Constants.Formats.CHAT_TIME, Locale.getDefault())
            return sdf.format(Date(ts))
        }
    }
}