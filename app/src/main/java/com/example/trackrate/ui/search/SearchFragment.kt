package com.example.trackrate.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.DetailActivity
import com.example.trackrate.R
import com.example.trackrate.databinding.FragmentSearchBinding
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.ui.catalog.CatalogAdapter
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: CatalogAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CatalogAdapter { item ->
            startActivity(DetailActivity.newIntent(requireContext(), item.type, item.id))
        }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.searchInput.addTextChangedListener { text ->
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }

        binding.typeChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val type = when (checkedIds.firstOrNull()) {
                R.id.chip_artists -> CatalogType.ARTIST
                R.id.chip_albums -> CatalogType.ALBUM
                R.id.chip_tracks -> CatalogType.TRACK
                else -> null
            }
            viewModel.onTypeSelected(type)
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.items)

                    val showEmpty = !state.isLoading && state.hasSearched && state.items.isEmpty()
                    binding.emptyView.visibility = if (showEmpty) View.VISIBLE else View.GONE

                    state.message?.let { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
