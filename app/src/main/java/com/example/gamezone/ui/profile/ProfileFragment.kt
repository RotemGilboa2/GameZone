package com.example.gamezone.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gamezone.R
import com.example.gamezone.databinding.FragmentProfileBinding
import com.example.gamezone.model.User
import com.example.gamezone.remote.FirestorePresence
import com.example.gamezone.repository.ProfileRepository
import com.example.gamezone.ui.auth.LoginActivity
import com.example.gamezone.ui.profile.avatar.AvatarPickerBottomSheet
import com.example.gamezone.utilities.Constants
import com.firebase.ui.auth.AuthUI
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth

// מציג את מסך הפרופיל, טוען את נתוני המשתמש דרך ProfileRepository,
// מאפשר לערוך ולשמור אווטאר, אזור, כמות משחקים ומשחקים מועדפים, ומבצע התנתקות מהחשבון.
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() =
        requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val profileRepo by lazy { ProfileRepository() }

    private var currentRegion: String? = null
    private var currentGamesCount: Int? = null
    private var currentFavGames: List<String> = emptyList()
    private val maxFavGames = 5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        initClicks()
        // מצב טעינה עד שמביאים פרופיל
        binding.profileScroll.visibility = View.INVISIBLE
        binding.profileLoading.visibility = View.VISIBLE

        loadUserProfile()

        return binding.root
    }

    private fun initClicks() {
        // עריכת אווטאר
        binding.profileBtnEdit.setOnClickListener {
            AvatarPickerBottomSheet { selected ->
                setAvatarByName(selected)

                val uid = auth.currentUser?.uid ?: return@AvatarPickerBottomSheet
                profileRepo.saveAvatar(uid, selected) {  }
            }.show(parentFragmentManager, Constants.UiTags.AVATAR_PICKER)
        }
        // עריכת אזור
        binding.profileCardRegion.setOnClickListener {
            RegionPickerBottomSheet(currentRegion) { picked ->
                saveRegion(picked)
            }.show(parentFragmentManager, Constants.UiTags.AVATAR_PICKER)
        }

        binding.profileCardGamesCount.setOnClickListener {
            showGamesCountDialog()
        }

        binding.profileCardFav.setOnClickListener { showFavoriteGamesDialog() }
        binding.profileBtnEditFav.setOnClickListener { showFavoriteGamesDialog() }

        binding.profileBtnLogout.setOnClickListener { signOutUser() }
    }

    // טוען את הפרופיל דרך הריפו ומעדכן UI
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        profileRepo.loadProfile(uid, auth.currentUser?.displayName) { user, error ->
            if (!isAdded) return@loadProfile

            if (error != null || user == null) {
                Toast.makeText(requireContext(), getString(R.string.profile_failed_load),
                    Toast.LENGTH_LONG).show()
                binding.profileLoading.visibility = View.GONE
                binding.profileScroll.visibility = View.VISIBLE
                return@loadProfile
            }

            bindUserToUi(user)

            binding.profileLoading.visibility = View.GONE
            binding.profileScroll.visibility = View.VISIBLE
        }
    }

    // לוקחת את נתוני המשתמש ומעדכנת לפיהם את כל רכיבי הפרופיל במסך
    private fun bindUserToUi(user: User) {
        currentFavGames = user.favoriteGames

        binding.profileLblUsername.text = user.username
        setAvatarByName(user.avatarRes)

        currentRegion = user.region
        currentGamesCount = user.gamesCount

        binding.profileLblRegionValue.text = user.region
        binding.profileLblGamesValue.text = user.gamesCount.toString()

        renderFavoriteChips(user.favoriteGames)
    }

    private fun renderFavoriteChips(games: List<String>) {
        binding.profileChipGroupFav.removeAllViews()

        if (games.isEmpty()) {
            binding.profileLblNoFav.visibility = View.VISIBLE
            return
        } else {
            binding.profileLblNoFav.visibility = View.GONE
        }

        games.forEach { game ->
            val chip = Chip(requireContext()).apply {
                text = game
                isClickable = false
                isCheckable = false

                setTextColor(resources.getColor(R.color.text_primary, null))
                chipBackgroundColor = resources.getColorStateList(R.color.card_inner, null)
                chipStrokeWidth = 1f
                chipStrokeColor = resources.getColorStateList(R.color.nav_selected, null)

                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(999f)
                    .build()

                textSize = 13f
                setPadding(8, 6, 8, 6)
            }

            binding.profileChipGroupFav.addView(chip)
        }
    }
    // דיאלוג לבחירת המשחקים המועדפים עם הגבלה עד 5
    private fun showFavoriteGamesDialog() {
        val uid = auth.currentUser?.uid ?: return

        val allGames = resources.getStringArray(R.array.games_catalog)
        val checked = BooleanArray(allGames.size) { i -> currentFavGames.contains(allGames[i]) }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_favorite_games_title, maxFavGames))
            .setMultiChoiceItems(allGames, checked) { _, which, isChecked ->
                if (isChecked) {
                    val count = checked.count { it }
                    if (count > maxFavGames) {
                        checked[which] = false
                        Toast.makeText(requireContext(), getString(R.string.profile_favorite_games_limit, maxFavGames),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val selected = allGames.filterIndexed { idx, _ -> checked[idx] }

                currentFavGames = selected
                renderFavoriteChips(selected)

                profileRepo.saveFavoriteGames(uid, selected) { e ->
                    if (e == null) {
                        Toast.makeText(requireContext(), getString(R.string.profile_favorite_games_updated),
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_save_failed,
                            e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun saveRegion(region: String) {
        val uid = auth.currentUser?.uid ?: return

        profileRepo.saveRegion(uid, region) { e ->
            if (e == null) {
                currentRegion = region
                binding.profileLblRegionValue.text = region
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_save_failed,
                    e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
            }
        }
    }
    // דיאלוג להזנת כמות משחקים (חייב חיובי)
    private fun showGamesCountDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.dialog_games_count_hint)
            setText(currentGamesCount?.toString() ?: "0")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_games_count_title))
            .setView(input)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_save)) { _, _ ->
                val value = input.text.toString().trim().toIntOrNull()
                if (value == null || value < 0) {
                    Toast.makeText(requireContext(), getString(R.string.profile_invalid_number),
                        Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveGamesCount(value)
            }
            .show()
    }

    private fun saveGamesCount(count: Int) {
        val uid = auth.currentUser?.uid ?: return

        profileRepo.saveGamesCount(uid, count) { e ->
            if (e == null) {
                currentGamesCount = count
                binding.profileLblGamesValue.text = count.toString()
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_save_failed,
                    e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setAvatarByName(drawableName: String) {
        val resId = resources.getIdentifier(drawableName, Constants.Resources.DRAWABLE,
            requireContext().packageName)
        binding.profileImgAvatar.setImageResource(if (resId != 0) resId else R.drawable.logo)
    }

    // התנתקות: מסמן אופחיין, עושה התנתקות דרך FB UI ומעביר למסך לוגין ומנקה backstack
    private fun signOutUser() {
        // חייב להיות מיידי לפני שמאבדים את uid
        FirestorePresence.setOfflineNow()

        AuthUI.getInstance()
            .signOut(requireContext())
            .addOnCompleteListener {
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.logout_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}