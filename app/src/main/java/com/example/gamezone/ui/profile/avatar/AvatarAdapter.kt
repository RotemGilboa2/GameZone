package com.example.gamezone.ui.profile.avatar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.databinding.ItemAvatarBinding
import com.example.gamezone.utilities.Constants

// אדפטר שמציג את רשימת האווטארים לבחירה במסך הפרופיל
class AvatarAdapter(
    private val items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.AvatarVH>() {

    inner class AvatarVH(val binding: ItemAvatarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarVH {
        val binding = ItemAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AvatarVH(binding)
    }

    override fun onBindViewHolder(holder: AvatarVH, position: Int) {
        val name = items[position]
        val ctx = holder.binding.root.context
        val resId = ctx.resources.getIdentifier(name, Constants.Resources.DRAWABLE,
            ctx.packageName)

        holder.binding.itemAvatarImg.setImageResource(resId)
        holder.binding.itemAvatarImg.setOnClickListener { onClick(name) }
    }

    override fun getItemCount(): Int = items.size
}
