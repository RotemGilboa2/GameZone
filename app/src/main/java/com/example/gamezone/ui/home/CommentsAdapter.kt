package com.example.gamezone.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.model.Comment
import com.example.gamezone.databinding.ItemCommentBinding
import com.example.gamezone.utilities.Constants
import com.example.gamezone.R


// מציג ב-RecyclerView את רשימת התגובות לפוסט, כולל שם המשתמש, הטקסט, האווטאר וזמן הפרסום
class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.CommentVH>() {

    private val items = ArrayList<Comment>()
    // מקבל רשימת תגובות ומעדכן אותה
    fun submitList(newList: List<Comment>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    // יוצר את השכבה של השורת תגובה
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val b = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent,
            false)
        return CommentVH(b)
    }

    // מחבר את הנתונים של התגובה לשורה
    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // מציג בפועל את התגובה התמונה זמן ומשתמש
    inner class CommentVH(private val b: ItemCommentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: Comment) {
            val ctx = b.root.context

            b.commentLblName.text = c.authorName
            b.commentLblText.text = c.text

            // avatar
            val resId = ctx.resources.getIdentifier(c.authorAvatarRes, Constants.Resources.DRAWABLE,
                ctx.packageName)
            b.commentImgAvatar.setImageResource(if (resId != 0) resId else R.drawable.logo)

            // time ago
            val millis = c.createdAt?.toDate()?.time ?: 0L
            b.commentLblTime.text = timeAgo(ctx,millis)
        }

    }
    private fun timeAgo(ctx: android.content.Context, timeMillis: Long): String {
        if (timeMillis <= 0L) return ""

        val now = System.currentTimeMillis()
        val diff = (now - timeMillis).coerceAtLeast(0L)

        val min = diff / 60_000
        val hour = diff / 3_600_000
        val day = diff / 86_400_000

        return when {
            min < 1 -> ctx.getString(R.string.time_just_now)
            min < 60 -> ctx.getString(R.string.time_minutes_ago, min)
            hour < 24 -> ctx.getString(R.string.time_hours_ago, hour)
            else -> ctx.getString(R.string.time_days_ago, day)
        }
    }

}
