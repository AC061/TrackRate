package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.example.trackrate.ui.ThemedAppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trackrate.databinding.ActivityAdminUsersBinding
import com.example.trackrate.ui.admin.AdminUsersViewModel
import com.example.trackrate.util.setBrandedTitle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminUsersActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivityAdminUsersBinding
    private val viewModel: AdminUsersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setBrandedTitle(R.string.admin_users_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.grantButton.setOnClickListener {
            viewModel.setAdmin(binding.usernameInput.text?.toString().orEmpty(), makeAdmin = true)
        }
        binding.revokeButton.setOnClickListener {
            viewModel.setAdmin(binding.usernameInput.text?.toString().orEmpty(), makeAdmin = false)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isWorking) View.VISIBLE else View.GONE
                    binding.grantButton.isEnabled = !state.isWorking
                    binding.revokeButton.isEnabled = !state.isWorking
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, AdminUsersActivity::class.java)
    }
}
