package com.example.trackrate.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trackrate.R
import com.example.trackrate.databinding.FragmentSettingsBinding
import com.example.trackrate.domain.model.AccentColor
import com.example.trackrate.domain.model.AppPreferences
import com.example.trackrate.domain.model.AppTextSize
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    private var isApplyingPreferences = false
    private var spinnerEventsEnabled = false
    private var darkModeListener: ((android.widget.CompoundButton, Boolean) -> Unit)? = null

    private val textSizeLabels by lazy {
        listOf(
            getString(R.string.settings_text_size_normal),
            getString(R.string.settings_text_size_large),
            getString(R.string.settings_text_size_extra_large)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isApplyingPreferences = true
        binding.textSizeSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, textSizeLabels).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        binding.textSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                itemView: View?,
                position: Int,
                id: Long
            ) {
                if (!spinnerEventsEnabled || isApplyingPreferences) return
                val size = AppTextSize.entries[position]
                if (size == viewModel.current().textSize) return
                viewModel.setTextSize(size)
                restartForAppearanceChange()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        darkModeListener = { _, isChecked ->
            if (!isApplyingPreferences && isChecked != viewModel.current().darkMode) {
                isApplyingPreferences = true
                viewModel.setDarkMode(isChecked)
                binding.root.post { isApplyingPreferences = false }
            }
        }
        binding.darkModeSwitch.setOnCheckedChangeListener(darkModeListener)

        binding.accentPurpleButton.setOnClickListener { selectAccent(AccentColor.PURPLE) }
        binding.accentBlueButton.setOnClickListener { selectAccent(AccentColor.BLUE) }
        binding.accentRedButton.setOnClickListener { selectAccent(AccentColor.RED) }

        bindPreferences(viewModel.current())
        binding.textSizeSpinner.post {
            isApplyingPreferences = false
            spinnerEventsEnabled = true
        }

        observePreferences()
    }

    private fun selectAccent(color: AccentColor) {
        if (isApplyingPreferences) return
        if (color == viewModel.current().accentColor) return
        viewModel.setAccentColor(color)
        restartForAppearanceChange()
    }

    private fun restartForAppearanceChange() {
        val host = activity ?: return
        if (host.isFinishing || host.isDestroyed) return
        host.recreate()
    }

    private fun observePreferences() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.preferences.collect { prefs ->
                    bindPreferences(prefs)
                }
            }
        }
    }

    private fun bindPreferences(prefs: AppPreferences) {
        isApplyingPreferences = true
        binding.darkModeSwitch.setOnCheckedChangeListener(null)
        binding.darkModeSwitch.isChecked = prefs.darkMode
        binding.darkModeSwitch.setOnCheckedChangeListener(darkModeListener)
        if (binding.textSizeSpinner.selectedItemPosition != prefs.textSize.ordinal) {
            binding.textSizeSpinner.setSelection(prefs.textSize.ordinal, false)
        }
        updateAccentButtons(prefs.accentColor)
        binding.root.post { isApplyingPreferences = false }
    }

    private fun updateAccentButtons(selected: AccentColor) {
        val strokeSelected = resources.getDimensionPixelSize(R.dimen.accent_button_stroke_selected)
        val strokeNormal = resources.getDimensionPixelSize(R.dimen.accent_button_stroke_normal)

        fun styleButton(button: MaterialButton, accent: AccentColor, colorRes: Int) {
            val isSelected = selected == accent
            val accentColor = ContextCompat.getColor(requireContext(), colorRes)
            button.isCheckable = false
            button.rippleColor = android.content.res.ColorStateList.valueOf(accentColor)
            button.strokeColor = android.content.res.ColorStateList.valueOf(accentColor)
            button.strokeWidth = if (isSelected) strokeSelected else strokeNormal
            button.alpha = if (isSelected) 1f else 0.72f
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isSelected) ColorUtils.setAlphaComponent(accentColor, 40) else Color.TRANSPARENT
            )
        }

        styleButton(binding.accentPurpleButton, AccentColor.PURPLE, R.color.accent_purple)
        styleButton(binding.accentBlueButton, AccentColor.BLUE, R.color.accent_blue)
        styleButton(binding.accentRedButton, AccentColor.RED, R.color.accent_red)
    }

    override fun onDestroyView() {
        spinnerEventsEnabled = false
        darkModeListener = null
        super.onDestroyView()
        _binding = null
    }
}
