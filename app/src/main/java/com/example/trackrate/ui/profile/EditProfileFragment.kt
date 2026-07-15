package com.example.trackrate.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.trackrate.databinding.ActivityEditProfileBinding
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: ActivityEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.uploadAvatar(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityEditProfileBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.content.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    binding.saveButton.isEnabled = !state.isSaving && !state.isUploadingAvatar
                    binding.changeAvatarButton.isEnabled = !state.isUploadingAvatar
                    binding.avatar.isEnabled = !state.isUploadingAvatar

                    state.profile?.let { profile ->
                        binding.avatar.load(profile.avatarUrl) {
                            placeholder(com.example.trackrate.R.drawable.ic_mdi_account)
                            error(com.example.trackrate.R.drawable.ic_mdi_account)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
