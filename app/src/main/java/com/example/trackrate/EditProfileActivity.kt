package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.trackrate.ui.ThemedAppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.trackrate.databinding.ActivityEditProfileBinding
import com.example.trackrate.ui.profile.EditProfileViewModel
import com.example.trackrate.util.setBrandedTitle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.uploadAvatar(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setBrandedTitle(R.string.edit_profile_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.changeAvatarButton.setOnClickListener { pickAvatar.launch("image/*") }
        binding.avatar.setOnClickListener { pickAvatar.launch("image/*") }
        binding.saveButton.setOnClickListener {
            viewModel.saveProfile(
                username = binding.usernameInput.text?.toString().orEmpty(),
                displayName = binding.displayNameInput.text?.toString().orEmpty(),
                bio = binding.bioInput.text?.toString().orEmpty()
            )
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.content.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    binding.saveButton.isEnabled = !state.isSaving && !state.isUploadingAvatar
                    binding.changeAvatarButton.isEnabled = !state.isUploadingAvatar
                    binding.avatar.isEnabled = !state.isUploadingAvatar

                    state.profile?.let { profile ->
                        binding.avatar.load(profile.avatarUrl) {
                            placeholder(R.drawable.ic_mdi_account)
                            error(R.drawable.ic_mdi_account)
                        }
                        if (binding.usernameInput.text.isNullOrEmpty()) {
                            binding.usernameInput.setText(profile.username)
                        }
                        if (binding.displayNameInput.text.isNullOrEmpty()) {
                            binding.displayNameInput.setText(profile.displayName.orEmpty())
                        }
                        if (binding.bioInput.text.isNullOrEmpty()) {
                            binding.bioInput.setText(profile.bio.orEmpty())
                        }
                    }

                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, EditProfileActivity::class.java)
    }
}
