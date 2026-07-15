package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trackrate.databinding.ActivityChangePasswordBinding
import com.example.trackrate.ui.auth.ChangePasswordViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.submitButton.setOnClickListener {
            viewModel.submit(
                currentPassword = binding.currentPasswordInput.text
                    ?.toString()
                    .orEmpty(),
                newPassword = binding.newPasswordInput.text
                    ?.toString()
                    .orEmpty(),
                confirmPassword = binding.confirmPasswordInput.text
                    ?.toString()
                    .orEmpty()
            )
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateLoadingState(state.isSubmitting)

                    state.errorMessage?.let { message ->
                        Snackbar.make(
                            binding.root,
                            message,
                            Snackbar.LENGTH_LONG
                        ).show()

                        viewModel.consumeError()
                    }

                    state.successMessage?.let { message ->
                        showSuccessDialog(message)
                        viewModel.consumeSuccess()
                    }
                }
            }
        }
    }

    private fun updateLoadingState(isSubmitting: Boolean) {
        binding.progress.visibility =
            if (isSubmitting) View.VISIBLE else View.GONE

        binding.submitButton.isEnabled = !isSubmitting
        binding.cancelButton.isEnabled = !isSubmitting
        binding.currentPasswordInput.isEnabled = !isSubmitting
        binding.newPasswordInput.isEnabled = !isSubmitting
        binding.confirmPasswordInput.isEnabled = !isSubmitting
    }

    private fun showSuccessDialog(message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.change_password_success_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                finish()
            }
            .show()
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, ChangePasswordActivity::class.java)
    }
}
