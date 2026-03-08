package com.example.gamezone.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.R
import com.example.gamezone.databinding.ItemPostBinding
import com.example.gamezone.model.Post
import com.example.gamezone.utilities.Constants

// האדפטר של הפיד, כלומר המחלקה שאחראית להציג את רשימת הפוסטים ב-RecyclerView ולחבר כל פוסט ל־UI שלו
class PostsAdapter(
    private val onLike: (Post) -> Unit,
    private val onComments: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostVH>() {

    private val items = ArrayList<Post>()
    private val likedByMe = HashSet<String>() // postId שעשיתי עליו לייק


    fun submitList(newList: List<Post>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

     // מעדכן סטייט לייק לפוסט ספציפי ומרענן רק את השורה שלו
    fun setLiked(postId: String, liked: Boolean) {
        if (liked) likedByMe.add(postId) else likedByMe.remove(postId)
        val idx = items.indexOfFirst { it.id == postId }
        if (idx != -1) notifyItemChanged(idx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH {
        val b = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostVH(b)
    }

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PostVH(private val b: ItemPostBinding) : RecyclerView.ViewHolder(b.root) {
//          קושר Post לשורה:
//           שם, טקסט, אווטאר
//           זמן "לפני כמה זמן"
//           מספר לייקים/תגובות
//           סטייט אייקון לייק + קליקים
        fun bind(p: Post) {
            val ctx = b.root.context

            b.postLblName.text = p.authorName
            b.postLblText.text = p.text
            b.postLblLikes.text = p.likeCount.toString()
            b.postLblComments.text = p.commentCount.toString()

            // אווטאר: שם drawable מתוך authorAvatarRes
            val resId = ctx.resources.getIdentifier(
                p.authorAvatarRes,
                Constants.Resources.DRAWABLE,
                ctx.packageName
            )
            b.postImgAvatar.setImageResource(if (resId != 0) resId else R.drawable.logo)

            // זמן: createdAt (Timestamp) -> millis -> timeAgo
            val millis = p.createdAt?.toDate()?.time ?: 0L
            b.postLblTime.text = timeAgo(ctx, millis)

            // סטייט אייקון לייק
            val isLiked = likedByMe.contains(p.id)
            b.postBtnLike.setImageResource(
                if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
            )

            // קליק על לייק: אנימציה + עדכון מיידי ב-UI + callback
            b.postBtnLike.setOnClickListener {
                animateLike(b.postBtnLike)

                val nowLiked = !likedByMe.contains(p.id)
                setLiked(p.id, nowLiked)

                onLike(p)
            }

            // קליק על תגובות
            b.postBtnComments.setOnClickListener { onComments(p) }
        }

        private fun animateLike(view: View) {
            view.animate().cancel()

            view.scaleX = 1f
            view.scaleY = 1f

            view.animate()
                .scaleX(1.18f)
                .scaleY(1.18f)
                .setDuration(120)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
                .start()
        }
    }

    private fun timeAgo(ctx: Context, timeMillis: Long): String {
        if (timeMillis <= 0L) return ""

        val now = System.currentTimeMillis()
        val diff = (now - timeMillis).coerceAtLeast(0L)

        val min = (diff / 60_000).toInt()
        val hour = (diff / 3_600_000).toInt()
        val day = (diff / 86_400_000).toInt()

        return when {
            min < 1 -> ctx.getString(R.string.time_just_now)
            min < 60 -> ctx.getString(R.string.time_minutes_ago, min)
            hour < 24 -> ctx.getString(R.string.time_hours_ago, hour)
            else -> ctx.getString(R.string.time_days_ago, day)
        }
    }
}