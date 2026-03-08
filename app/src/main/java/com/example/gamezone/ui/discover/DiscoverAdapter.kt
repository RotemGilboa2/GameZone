package com.example.gamezone.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.R
import com.example.gamezone.model.User
import com.example.gamezone.databinding.ItemDiscoverUserBinding
import com.example.gamezone.utilities.Constants

// מציג ב-RecyclerView את רשימת המשתמשים שהתקבלה מה-Repository ומאפשר לשלוח בקשת חברות באמצעות כפתור Add
class DiscoverAdapter(
    private val onAddClick: (User) -> Unit
) : RecyclerView.Adapter<DiscoverAdapter.UserViewHolder>() {

    private val usersList = ArrayList<User>()

    fun updateList(newList: List<User>) {
        usersList.clear()
        usersList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemDiscoverUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(usersList[position])
    }

    override fun getItemCount(): Int = usersList.size

    inner class UserViewHolder(private val binding: ItemDiscoverUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

            // קושר שם משתמש ותמונה והוספה
        fun bind(user: User) {
            binding.itemLblUsername.text = user.username

            val context = binding.root.context
            val avatarName = user.avatarRes.ifBlank { Constants.Defaults.FALLBACK_AVATAR_DRAWABLE }
            val resId = context.resources.getIdentifier(avatarName, Constants.Resources.DRAWABLE, context.packageName)

            if (resId != 0) {
                binding.itemImgAvatar.setImageResource(resId)
            } else {
                binding.itemImgAvatar.setImageResource(R.drawable.logo)
            }

            binding.itemBtnAdd.setOnClickListener {
                onAddClick(user)
            }
        }
    }
}