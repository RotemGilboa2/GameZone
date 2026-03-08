package com.example.gamezone.ui.profile.avatar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gamezone.R
import com.example.gamezone.databinding.BottomsheetAvatarPickerBinding
import com.example.gamezone.repository.AvatarRepository
import com.example.gamezone.utilities.Constants
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

// מציג חלון בחירת אווטארים, מאפשר למשתמש לבחור אווטאר ושומר אותו בפרופיל דרך AvatarRepository
class AvatarPickerBottomSheet(
    private val onAvatarSelected: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetAvatarPickerBinding? = null
    private val binding get() =
        requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val repo by lazy { AvatarRepository() }

    private val avatars = listOf(
        "avatar_01",
        "avatar_02",
        "avatar_03",
        "avatar_04",
        "avatar_05",
        "avatar_06",
        "avatar_07",
        "avatar_08",
        "avatar_09",
        "avatar_10",
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetAvatarPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.avatarBtnClose.setOnClickListener { dismiss() }

        binding.avatarRv.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.avatarRv.adapter = AvatarAdapter(avatars) { selected ->
            saveAvatar(selected)
        }
    }

    private fun saveAvatar(selected: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), getString(R.string.error_not_logged_in),
                Toast.LENGTH_SHORT).show()
            return
        }

        repo.saveAvatar(uid, selected) { e ->
            if (e == null) {
                onAvatarSelected(selected)
                dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_save_failed,
                    e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}