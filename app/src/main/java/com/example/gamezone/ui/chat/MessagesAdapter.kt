package com.example.gamezone.ui.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.model.Message
import com.example.gamezone.databinding.ItemMessageMeBinding
import com.example.gamezone.databinding.ItemMessageOtherBinding
import com.example.gamezone.utilities.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// האדפטר שמציג את ההודעות בתוך הצ’אט ב-RecyclerView בהתאם אם זה שלי או של הצד השני
class MessagesAdapter(
    private val myUid: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Message>() {

            // בודק אם התוכן השתנה
            override fun areItemsTheSame(old: Message, new: Message): Boolean {
                return old.fromUid == new.fromUid &&
                        old.timestamp == new.timestamp &&
                        old.text == new.text
            }
            // אין שינוי בתוכן
            override fun areContentsTheSame(old: Message, new: Message): Boolean = old == new
        }
    }
    // איזה סוג layout, אני זה 1 הצד השני זה 2
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).fromUid == myUid) 1 else 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1) {
            val b = ItemMessageMeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MeVH(b)
        } else {
            val b = ItemMessageOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            OtherVH(b)
        }
    }

    // מכניס את ההודעה לviewholder המתאים
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        if (holder is MeVH) holder.bind(msg)
        if (holder is OtherVH) holder.bind(msg)
    }

    private fun formatTime(ts: Long): String {
        if (ts == 0L) return ""
        val sdf = SimpleDateFormat(Constants.Formats.MESSAGE_TIME_24H, Locale.getDefault())
        return sdf.format(Date(ts))
    }

    inner class MeVH(private val b: ItemMessageMeBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.msgText.text = msg.text
            b.msgTime.text = formatTime(msg.timestamp)
        }
    }

    inner class OtherVH(private val b: ItemMessageOtherBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.msgText.text = msg.text
            b.msgTime.text = formatTime(msg.timestamp)
        }
    }
}
