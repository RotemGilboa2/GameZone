package com.example.gamezone.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.gamezone.databinding.BottomsheetRegionPickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.gamezone.R
import com.example.gamezone.utilities.Constants

// מציג חלון לבחירת אזור מהרשימה ומחזיר את האזור שנבחר למסך הפרופיל
class RegionPickerBottomSheet(
    private val current: String?,
    private val onPick: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetRegionPickerBinding? = null
    private val binding get() =
        requireNotNull(_binding) { Constants.Debug.BINDING_OUTSIDE_LIFECYCLE }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomsheetRegionPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = resources.getStringArray(R.array.regions_catalog).toList()
        binding.regionList.removeAllViews()

        for (r in items) {
            val row = layoutInflater.inflate(
                android.R.layout.simple_list_item_1,
                binding.regionList,
                false
            )

            row.findViewById<android.widget.TextView>(android.R.id.text1).apply {
                text = if (r == current) {
                    getString(R.string.region_selected_prefix, r)
                } else {
                    r
                }
            }

            row.setOnClickListener {
                onPick(r)
                dismiss()
            }

            binding.regionList.addView(row)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
