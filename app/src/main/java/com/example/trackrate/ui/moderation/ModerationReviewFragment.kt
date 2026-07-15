package com.example.trackrate.ui.moderation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.DetailActivity
import com.example.trackrate.MainActivity
import com.example.trackrate.R
import com.example.trackrate.databinding.ActivityModerationReviewBinding
import com.example.trackrate.domain.model.CatalogDetail
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.ui.detail.ContributorAdapter
import com.example.trackrate.ui.detail.SampleAdapter
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModerationReviewFragment : Fragment() {

    private var _binding: ActivityModerationReviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ModerationReviewViewModel by viewModels()
    private lateinit var contributorAdapter: ContributorAdapter
    private lateinit var sampleAdapter: SampleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityModerationReviewBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val type = CatalogType.valueOf(
            arguments?.getString(TrackRateNavigation.ARG_ENTITY_TYPE) ?: CatalogType.ARTIST.name
        )
        val id = arguments?.getString(TrackRateNavigation.ARG_ENTITY_ID).orEmpty()

        contributorAdapter = ContributorAdapter { artistId ->
            startActivity(DetailActivity.newIntent(requireContext(), CatalogType.ARTIST, artistId))
        }
        sampleAdapter = SampleAdapter { trackId ->
            startActivity(DetailActivity.newIntent(requireContext(), CatalogType.TRACK, trackId))
        }
        binding.contributorsList.layoutManager = LinearLayoutManager(requireContext())
        binding.contributorsList.adapter = contributorAdapter
        binding.samplesList.layoutManager = LinearLayoutManager(requireContext())
        binding.samplesList.adapter = sampleAdapter

        binding.approveButton.setOnClickListener { viewModel.approve() }
        binding.rejectButton.setOnClickListener { showRejectDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.finished.collect {
                    Snackbar.make(binding.root, R.string.mod_action_done, Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

        viewModel.load(type, id)
    }

    private fun bind(detail: CatalogDetail, type: CatalogType) {
        binding.typeLabel.setText(type.labelRes())
        binding.title.text = detail.title
        (requireActivity() as? MainActivity)?.setToolbarBrandedTitle(detail.title)

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
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.mod_reject_reason_hint)
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(requireContext())
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
