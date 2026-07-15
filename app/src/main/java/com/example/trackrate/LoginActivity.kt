package com.example.trackrate

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.example.trackrate.ui.ThemedAppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trackrate.databinding.ActivityLoginBinding
import com.example.trackrate.domain.model.SessionStatus
import com.example.trackrate.ui.auth.AuthEvent
import com.example.trackrate.ui.auth.AuthMode
import com.example.trackrate.ui.auth.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.bootstrap()

        binding.submitButton.setOnClickListener {
            viewModel.submit(
                email = binding.emailInput.text?.toString().orEmpty(),
                password = binding.passwordInput.text?.toString().orEmpty(),
                confirmPassword = binding.confirmPasswordInput.text?.toString().orEmpty()
            )
        }

        binding.toggleModeButton.setOnClickListener {
            clearInputs()
            viewModel.toggleMode()
        }

        observeState()
        observeEvents()
        observeSession()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val isRegister = state.mode == AuthMode.REGISTER
                    binding.confirmPasswordLayout.visibility =
                        if (isRegister) android.view.View.VISIBLE else android.view.View.GONE
                    binding.titleText.setText(
                        if (isRegister) R.string.auth_register_title else R.string.auth_login_title
                    )
                    binding.submitButton.setText(
                        if (isRegister) R.string.auth_register_action else R.string.auth_login_action
                    )
                    binding.toggleModeButton.setText(
                        if (isRegister) R.string.auth_toggle_to_login else R.string.auth_toggle_to_register
                    )

                    binding.progress.visibility =
                        if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE
                    binding.submitButton.isEnabled = !state.isLoading
                    binding.toggleModeButton.isEnabled = !state.isLoading

                    state.errorMessage?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        AuthEvent.LoginSuccess,
                        AuthEvent.RegisterSuccess -> goToMain()
                    }
                }
            }
        }
    }

    private fun observeSession() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sessionStatus.collect { status ->
                    if (status is SessionStatus.Authenticated) {
                        goToMain()
                    }
                }
            }
        }
    }

    private fun clearInputs() {
        binding.emailInput.text?.clear()
        binding.passwordInput.text?.clear()
        binding.confirmPasswordInput.text?.clear()
        binding.emailInput.clearFocus()
        binding.passwordInput.clearFocus()
        binding.confirmPasswordInput.clearFocus()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
