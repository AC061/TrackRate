package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trackrate.databinding.ActivityChangePasswordBinding
import com.example.trackrate.ui.ThemedAppCompatActivity
import com.example.trackrate.ui.auth.ChangePasswordViewModel
import com.example.trackrate.util.setBrandedTitle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setBrandedTitle(R.string.change_password_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.saveButton.setOnClickListener {
            viewModel.changePassword(
                currentPassword = binding.currentPasswordInput.text?.toString().orEmpty(),
                newPassword = binding.newPasswordInput.text?.toString().orEmpty(),
                confirmPassword = binding.confirmPasswordInput.text?.toString().orEmpty()
            )
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isSaving) View.VISIBLE else View.GONE
                    binding.saveButton.isEnabled = !state.isSaving

                    state.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                        if (state.success) {
                            finish()
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, ChangePasswordActivity::class.java)
    }
}
