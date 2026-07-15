package com.example.trackrate.ui.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.DetailActivity
import com.example.trackrate.databinding.FragmentDiaryBinding
import com.example.trackrate.util.bindLoadError
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DiaryFragment : Fragment() {

    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DiaryViewModel by viewModels()
    private val adapter by lazy {
        DiaryAdapter { entry ->
            startActivity(DetailActivity.newIntent(requireContext(), entry.entityType, entry.entityId))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
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
                    val hasLoadError = !state.loadError.isNullOrBlank()
                    binding.loadError.bindLoadError(
                        errorMessage = state.loadError,
                        showRetry = state.canRetry,
                        onRetry = viewModel::load
                    )
                    adapter.submitList(state.entries)
                    binding.recycler.visibility = if (hasLoadError) View.GONE else View.VISIBLE
                    binding.emptyView.visibility =
                        if (!state.isLoading && !hasLoadError && state.entries.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
