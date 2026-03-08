package com.example.gamezone.ui.squad

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.R
import com.example.gamezone.model.FriendRequest
import com.example.gamezone.databinding.ItemFriendRequestBinding
import com.example.gamezone.utilities.Constants

// מציג ב-RecyclerView את בקשות החברות הנכנסות ומאפשר לאשר או לדחות כל בקשה
class RequestsAdapter(
    private val onAccept: (FriendRequest) -> Unit,
    private val onDecline: (FriendRequest) -> Unit
) : RecyclerView.Adapter<RequestsAdapter.ReqVH>() {

    private val items = ArrayList<FriendRequest>()

    fun submitList(newList: List<FriendRequest>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReqVH {
        val binding = ItemFriendRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReqVH(binding)
    }

    override fun onBindViewHolder(holder: ReqVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ReqVH(private val binding: ItemFriendRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // קושר בקשת חברות לשורה: שם שולח הבקשה, אווטר וכפתורים לאשר או לדחוץ
        fun bind(req: FriendRequest) {
            val ctx = binding.root.context

            binding.itemReqLblUsername.text =
                req.fromUsername.ifBlank { ctx.getString(R.string.default_username) }

            val name = req.fromAvatarRes.ifBlank { "logo" }
            val resId = ctx.resources.getIdentifier(name, Constants.Resources.DRAWABLE,
                ctx.packageName)
            binding.itemReqImgAvatar.setImageResource(if (resId != 0) resId else R.drawable.logo)

            binding.itemReqBtnAccept.setOnClickListener { onAccept(req) }
            binding.itemReqBtnDecline.setOnClickListener { onDecline(req) }
        }
    }
}
