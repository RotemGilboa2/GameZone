package com.example.gamezone.ui.squad

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.R
import com.example.gamezone.model.Friend
import com.example.gamezone.databinding.ItemFriendBinding
import com.example.gamezone.utilities.Constants

// מציג ב-RecyclerView את רשימת החברים, כולל שם,
// אווטאר וסטטוס Online/Offline, ומאפשר לפתוח צ'אט עם חבר.
class FriendsAdapter(
    private val onChatClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendVH>() {

    private val items = ArrayList<Friend>()

    fun submitList(newList: List<Friend>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    // עדכון נוכחות/אווטאר בזמן אמת
    fun updateFriend(updated: Friend) {
        val idx = items.indexOfFirst { it.uid == updated.uid }
        if (idx != -1) {
            items[idx] = updated
            notifyItemChanged(idx)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendVH {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendVH(binding)
    }

    override fun onBindViewHolder(holder: FriendVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FriendVH(private val b: ItemFriendBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(friend: Friend) {
            b.itemLblName.text = friend.username

            // Online/Offline
            val ctx = b.root.context

            b.itemLblStatus.text = if (friend.online) {
                ctx.getString(R.string.chat_status_online)
            } else {
                ctx.getString(R.string.chat_status_offline)
            }
            b.itemLblStatus.setTextColor(
                if (friend.online) ctx.getColor(R.color.online_green)
                else ctx.getColor(R.color.offline_gray)
            )

            val resId = ctx.resources.getIdentifier(friend.avatarRes, Constants.Resources.DRAWABLE,
                ctx.packageName)
            b.itemImgAvatar.setImageResource(if (resId != 0) resId else R.drawable.logo)

            b.itemBtnChat.setOnClickListener { onChatClick(friend) }
        }
    }
}
