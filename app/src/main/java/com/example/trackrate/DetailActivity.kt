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
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.trackrate.databinding.ActivityDetailBinding
import com.example.trackrate.databinding.DialogRatingBinding
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.Rating
import com.example.trackrate.domain.model.RatingStats
import com.example.trackrate.ui.detail.ContributorAdapter
import com.example.trackrate.ui.detail.DetailViewModel
import com.example.trackrate.ui.detail.SampleAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private val viewModel: DetailViewModel by viewModels()
    private lateinit var contributorAdapter: ContributorAdapter
    private lateinit var sampleAdapter: SampleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val typeName = intent.getStringExtra(EXTRA_TYPE) ?: CatalogType.ALBUM.name
        val type = CatalogType.valueOf(typeName)
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()

        contributorAdapter = ContributorAdapter { artistId ->
            startActivity(newIntent(this, CatalogType.ARTIST, artistId))
        }
        sampleAdapter = SampleAdapter { trackId ->
            startActivity(newIntent(this, CatalogType.TRACK, trackId))
        }
        binding.contributorsList.layoutManager = LinearLayoutManager(this)
        binding.contributorsList.adapter = contributorAdapter
        binding.samplesList.layoutManager = LinearLayoutManager(this)
        binding.samplesList.adapter = sampleAdapter

        binding.rateButton.setOnClickListener { showRatingDialog() }
        binding.deleteRatingButton.setOnClickListener { confirmDeleteRating() }
        binding.addToListButton.setOnClickListener { showAddToListDialog() }

        observeState(type)
        viewModel.load(type, id)
    }

    private fun observeState(type: CatalogType) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    binding.content.visibility =
                        if (state.detail != null) View.VISIBLE else View.GONE

                    state.detail?.let { bind(it, type) }
                    bindRatings(state.stats, state.myRating)

                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun bindRatings(stats: RatingStats?, myRating: Rating?) {
        if (stats != null && stats.count > 0) {
            binding.communityAverage.text = "%.1f".format(stats.average)
            binding.communityCount.text =
                resources.getQuantityString(R.plurals.rating_count, stats.count, stats.count)
        } else {
            binding.communityAverage.text = "—"
            binding.communityCount.text = getString(R.string.rating_none_yet)
        }

        if (myRating != null) {
            binding.myRatingBar.visibility = View.VISIBLE
            binding.myRatingBar.rating = myRating.rating.toFloat()
            binding.rateButton.setText(R.string.rating_edit_action)
            binding.deleteRatingButton.visibility = View.VISIBLE
            if (!myRating.review.isNullOrBlank()) {
                binding.myReview.visibility = View.VISIBLE
                binding.myReview.text = myRating.review
            } else {
                binding.myReview.visibility = View.GONE
            }
        } else {
            binding.myRatingBar.visibility = View.GONE
            binding.myReview.visibility = View.GONE
            binding.rateButton.setText(R.string.rating_rate_action)
            binding.deleteRatingButton.visibility = View.GONE
        }
    }

    private fun showRatingDialog() {
        val current = viewModel.uiState.value.myRating
        val dialogBinding = DialogRatingBinding.inflate(layoutInflater)
        dialogBinding.ratingBar.rating = current?.rating?.toFloat() ?: 0f
        dialogBinding.reviewInput.setText(current?.review.orEmpty())

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rating_dialog_title)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.settings_save) { _, _ ->
                val value = dialogBinding.ratingBar.rating
                if (value < 0.5f) {
                    Snackbar.make(binding.root, R.string.rating_min_required, Snackbar.LENGTH_SHORT)
                        .show()
                } else {
                    val listenedAt = current?.listenedAt ?: LocalDate.now().toString()
                    viewModel.saveRating(
                        rating = value.toDouble(),
                        review = dialogBinding.reviewInput.text?.toString(),
                        listenedAt = listenedAt
                    )
                }
            }
            .show()
    }

    private fun confirmDeleteRating() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rating_delete_action)
            .setMessage(R.string.rating_delete_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.rating_delete_action) { _, _ -> viewModel.deleteRating() }
            .show()
    }

    private fun showAddToListDialog() {
        lifecycleScope.launch {
            try {
                val lists = viewModel.getMyLists()
                if (lists.isEmpty()) {
                    Snackbar.make(binding.root, R.string.lists_empty_create_first, Snackbar.LENGTH_LONG)
                        .setAction(R.string.lists_create) {
                            startActivity(ListsActivity.newIntent(this@DetailActivity))
                        }
                        .show()
                    return@launch
                }
                val titles = lists.map { it.title }.toTypedArray()
                MaterialAlertDialogBuilder(this@DetailActivity)
                    .setTitle(R.string.lists_add_to)
                    .setItems(titles) { _, which -> viewModel.addToList(lists[which].id) }
                    .show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.message ?: "Error", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun bind(detail: CatalogDetail, type: CatalogType) {
        val placeholder = when (type) {
            CatalogType.ARTIST -> R.drawable.ic_mdi_account_music
            CatalogType.ALBUM -> R.drawable.ic_mdi_album
            CatalogType.TRACK -> R.drawable.ic_mdi_music_note
        }
        binding.cover.load(detail.imageUrl) {
            placeholder(placeholder)
            error(placeholder)
        }
        binding.title.text = detail.title
        binding.toolbar.title = detail.title

        binding.typeLabel.setText(
            when (type) {
                CatalogType.ARTIST -> R.string.catalog_type_artist
                CatalogType.ALBUM -> R.string.catalog_type_album
                CatalogType.TRACK -> R.string.catalog_type_track
            }
        )

        val subtitleParts = buildList {
            detail.subtitle?.let { add(it) }
            detail.extra?.let { add(it) }
            detail.year?.let { add(it.toString()) }
            detail.durationMs?.let { add(formatDuration(it)) }
        }
        binding.subtitle.text = subtitleParts.joinToString(" • ")
        binding.subtitle.visibility =
            if (subtitleParts.isEmpty()) View.GONE else View.VISIBLE

        binding.description.text = detail.description.orEmpty()
        binding.description.visibility =
            if (detail.description.isNullOrBlank()) View.GONE else View.VISIBLE

        val hasContributors = detail.contributors.isNotEmpty()
        binding.contributorsSectionTitle.visibility = if (hasContributors) View.VISIBLE else View.GONE
        binding.contributorsList.visibility = if (hasContributors) View.VISIBLE else View.GONE
        contributorAdapter.submitList(detail.contributors)

        val hasSamples = detail.samples.isNotEmpty()
        binding.samplesSectionTitle.visibility = if (hasSamples) View.VISIBLE else View.GONE
        binding.samplesList.visibility = if (hasSamples) View.VISIBLE else View.GONE
        sampleAdapter.submitList(detail.samples)

        if (detail.label.isNullOrBlank()) {
            binding.labelCredit.visibility = View.GONE
        } else {
            binding.labelCredit.visibility = View.VISIBLE
            binding.labelCredit.text = getString(R.string.detail_label_credit, detail.label)
        }
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
            Intent(context, DetailActivity::class.java).apply {
                putExtra(EXTRA_TYPE, type.name)
                putExtra(EXTRA_ID, id)
            }
    }
}
