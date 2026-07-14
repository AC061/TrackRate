package com.example.trackrate.ui.home

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
import com.example.trackrate.ProfileActivity
import com.example.trackrate.databinding.FragmentHomeBinding
import com.example.trackrate.domain.model.CatalogType
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val feedAdapter by lazy {
        FeedAdapter(
            onProfileClick = { item ->
                startActivity(ProfileActivity.newIntent(requireContext(), item.username))
            },
            onEntityClick = { item ->
                startActivity(DetailActivity.newIntent(requireContext(), item.entityType, item.entityId))
            }
        )
    }
    private val topRatedAdapter by lazy {
        TopRatedAdapter { track ->
            startActivity(DetailActivity.newIntent(requireContext(), CatalogType.TRACK, track.id))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = feedAdapter

        binding.topRatedRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.topRatedRecycler.adapter = topRatedAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    topRatedAdapter.submitList(state.topRated)
                    binding.topRatedEmpty.visibility =
                        if (!state.isLoading && state.topRated.isEmpty()) View.VISIBLE else View.GONE
                    feedAdapter.submitList(state.items)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE
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
