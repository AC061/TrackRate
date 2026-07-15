package com.example.trackrate.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.trackrate.R
import com.example.trackrate.databinding.ActivityEditProfileBinding
import com.example.trackrate.util.CameraCapture
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: ActivityEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()
    private var pendingCameraUri: Uri? = null

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.uploadAvatar(uri)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            viewModel.uploadAvatar(uri)
        }
        pendingCameraUri = null
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            val message = if (permanentlyDenied) {
                R.string.camera_permission_settings
            } else {
                R.string.camera_permission_denied
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
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

        binding.changeAvatarButton.setOnClickListener { showAvatarSourceDialog() }
        binding.avatar.setOnClickListener { showAvatarSourceDialog() }
        binding.changePasswordButton.setOnClickListener {
            TrackRateNavigation.navigateToChangePassword(this)
        }
        binding.saveButton.setOnClickListener {
            viewModel.saveProfile(
                username = binding.usernameInput.text?.toString().orEmpty(),
                displayName = binding.displayNameInput.text?.toString().orEmpty(),
                bio = binding.bioInput.text?.toString().orEmpty()
            )
        }

        observeState()
    }

    private fun showAvatarSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.photo_choose_source)
            .setItems(
                arrayOf(
                    getString(R.string.photo_from_gallery),
                    getString(R.string.photo_from_camera)
                )
            ) { _, which ->
                when (which) {
                    0 -> pickAvatar.launch("image/*")
                    1 -> requestCameraAccess()
                }
            }
            .show()
    }

    private fun requestCameraAccess() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> launchCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.camera_permission_rationale)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                    .show()
            }

            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val uri = CameraCapture.createTempImageUri(requireContext())
        pendingCameraUri = uri
        takePicture.launch(uri)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
