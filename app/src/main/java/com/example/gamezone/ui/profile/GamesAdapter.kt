package com.example.gamezone.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gamezone.model.GameItem
import com.example.gamezone.databinding.ItemGameCheckboxBinding

// מציג רשימת משחקים עם Checkbox לבחירת משחקים מועדפים,
// מגביל את מספר הבחירות המקסימלי ומחזיר את המשחקים שנבחרו
class GamesAdapter(
    private val items: List<GameItem>,
    private val maxSelection: Int,
    private val onLimitReached: () -> Unit
) : RecyclerView.Adapter<GamesAdapter.GameVH>() {

    inner class GameVH(val binding: ItemGameCheckboxBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameVH {
        val binding = ItemGameCheckboxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameVH(binding)
    }

    override fun onBindViewHolder(holder: GameVH, position: Int) {
        val item = items[position]

        holder.binding.itemGameCHK.text = item.name
        holder.binding.itemGameCHK.setOnCheckedChangeListener(null)
        holder.binding.itemGameCHK.isChecked = item.isSelected

        holder.binding.itemGameCHK.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val selectedCount = items.count { it.isSelected }
                if (selectedCount >= maxSelection) {
                    // ביטול הסימון כי עברנו את המקסימום
                    holder.binding.itemGameCHK.isChecked = false
                    onLimitReached()
                    return@setOnCheckedChangeListener
                }
            }
            item.isSelected = isChecked
        }
    }

    override fun getItemCount(): Int = items.size

    fun getSelectedGameNames(): List<String> {
        return items.filter { it.isSelected }.map { it.name }
    }
}
