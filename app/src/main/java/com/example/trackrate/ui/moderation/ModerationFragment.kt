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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.R
import com.example.trackrate.databinding.ActivityModerationBinding
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ModerationFragment : Fragment() {

    private var _binding: ActivityModerationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ModerationViewModel by viewModels()
    private val adapter by lazy {
        ModerationAdapter(
            onReview = { item ->
                TrackRateNavigation.navigateToModerationReview(this, item.type, item.id)
            },
            onApprove = { item -> viewModel.approve(item.type, item.id) },
            onReject = { item -> showRejectDialog(item) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityModerationBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
                    viewModel.reject(item.type, item.id, reason)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
