package com.example.trackrate.ui.lists

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.trackrate.DetailActivity
import com.example.trackrate.MainActivity
import com.example.trackrate.databinding.ActivityListDetailBinding
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListDetailFragment : Fragment() {

    private var _binding: ActivityListDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListDetailViewModel by viewModels()
    private val adapter = ListItemsAdapter { item ->
        startActivity(DetailActivity.newIntent(requireContext(), item.entityType, item.entityId))
    }

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.uploadCover(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityListDetailBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.uploadCoverButton.setOnClickListener { pickCover.launch("image/*") }

        val listId = arguments?.getString(TrackRateNavigation.ARG_LIST_ID).orEmpty()
        val listTitle = arguments?.getString(TrackRateNavigation.ARG_LIST_TITLE).orEmpty()
        (requireActivity() as? MainActivity)?.setToolbarBrandedTitle(listTitle)
        viewModel.init(listId, listTitle)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    (requireActivity() as? MainActivity)?.setToolbarBrandedTitle(state.listTitle)
                    binding.progress.visibility = if (state.isLoading || state.isUploadingCover) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    adapter.submitList(state.items)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE

                    if (state.coverUrl != null) {
                        binding.cover.visibility = View.VISIBLE
                        binding.cover.load(state.coverUrl)
                    }

                    binding.uploadCoverButton.isEnabled = !state.isUploadingCover

                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
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
