package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.ModerationReviewActivity
import com.example.trackrate.databinding.ActivityModerationBinding
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.ui.moderation.ModerationAdapter
import com.example.trackrate.ui.moderation.ModerationViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModerationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModerationBinding
    private val viewModel: ModerationViewModel by viewModels()
    private val adapter by lazy {
        ModerationAdapter(
            onReview = { item ->
                startActivity(ModerationReviewActivity.newIntent(this, item.type, item.id))
            },
            onApprove = { item -> viewModel.approve(item.type, item.id) },
            onReject = { item -> showRejectDialog(item) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModerationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.items)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun showRejectDialog(item: CatalogSubmission) {
        val input = EditText(this).apply {
            hint = getString(R.string.mod_reject_reason_hint)
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.mod_reject_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.mod_reject) { _, _ ->
                val reason = input.text?.toString()?.trim().orEmpty()
                if (reason.isEmpty()) {
                    Snackbar.make(binding.root, R.string.mod_reject_reason_required, Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.reject(item.type, item.id, reason)
                }
            }
            .show()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, ModerationActivity::class.java)
    }
}
