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
import com.example.trackrate.databinding.ActivityModerationReviewBinding
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.ui.detail.ContributorAdapter
import com.example.trackrate.ui.detail.SampleAdapter
import com.example.trackrate.ui.moderation.ModerationReviewViewModel
import com.example.trackrate.ui.moderation.labelRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModerationReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModerationReviewBinding
    private val viewModel: ModerationReviewViewModel by viewModels()
    private lateinit var contributorAdapter: ContributorAdapter
    private lateinit var sampleAdapter: SampleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModerationReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = CatalogType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: CatalogType.ARTIST.name)
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        contributorAdapter = ContributorAdapter { artistId ->
            startActivity(DetailActivity.newIntent(this, CatalogType.ARTIST, artistId))
        }
        sampleAdapter = SampleAdapter { trackId ->
            startActivity(DetailActivity.newIntent(this, CatalogType.TRACK, trackId))
        }
        binding.contributorsList.layoutManager = LinearLayoutManager(this)
        binding.contributorsList.adapter = contributorAdapter
        binding.samplesList.layoutManager = LinearLayoutManager(this)
        binding.samplesList.adapter = sampleAdapter

        binding.approveButton.setOnClickListener { viewModel.approve() }
        binding.rejectButton.setOnClickListener { showRejectDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.content.visibility = if (state.detail != null) View.VISIBLE else View.GONE
                    state.detail?.let { bind(it, type) }
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finished.collect {
                    Snackbar.make(binding.root, R.string.mod_action_done, Snackbar.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        viewModel.load(type, id)
    }

    private fun bind(detail: CatalogDetail, type: CatalogType) {
        binding.typeLabel.setText(type.labelRes())
        binding.title.text = detail.title
        binding.toolbar.title = detail.title

        val subtitleParts = buildList {
            detail.subtitle?.let { add(it) }
            detail.extra?.let { add(it) }
            detail.year?.let { add(it.toString()) }
            detail.durationMs?.let { add(formatDuration(it)) }
        }
        binding.subtitle.text = subtitleParts.joinToString(" • ")
        binding.subtitle.visibility = if (subtitleParts.isEmpty()) View.GONE else View.VISIBLE

        if (detail.description.isNullOrBlank()) {
            binding.description.visibility = View.GONE
        } else {
            binding.description.visibility = View.VISIBLE
            binding.description.text = detail.description
        }

        if (detail.label.isNullOrBlank()) {
            binding.labelText.visibility = View.GONE
        } else {
            binding.labelText.visibility = View.VISIBLE
            binding.labelText.text = getString(R.string.detail_label_credit, detail.label)
        }

        val hasContributors = detail.contributors.isNotEmpty()
        binding.contributorsSectionTitle.visibility = if (hasContributors) View.VISIBLE else View.GONE
        binding.contributorsList.visibility = if (hasContributors) View.VISIBLE else View.GONE
        contributorAdapter.submitList(detail.contributors)

        val hasSamples = detail.samples.isNotEmpty()
        binding.samplesSectionTitle.visibility = if (hasSamples) View.VISIBLE else View.GONE
        binding.samplesList.visibility = if (hasSamples) View.VISIBLE else View.GONE
        sampleAdapter.submitList(detail.samples)
    }

    private fun showRejectDialog() {
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
                    viewModel.reject(reason)
                }
            }
            .show()
    }

    private fun formatDuration(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    companion object {
        private const val EXTRA_TYPE = "extra_type"
        private const val EXTRA_ID = "extra_id"

        fun newIntent(context: Context, type: CatalogType, id: String): Intent =
            Intent(context, ModerationReviewActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type.name)
                putExtra(EXTRA_ID, id)
            }
    }
}
