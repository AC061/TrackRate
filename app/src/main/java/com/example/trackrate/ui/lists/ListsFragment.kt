package com.example.trackrate.ui.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.R
import com.example.trackrate.databinding.ActivityListsBinding
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListsFragment : Fragment() {

    private var _binding: ActivityListsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ListsViewModel by viewModels()
    private val adapter = ListsAdapter { list ->
        TrackRateNavigation.navigateToListDetail(this, list.id, list.title)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityListsBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.fab.setOnClickListener { showCreateDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.lists)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.lists.isEmpty()) View.VISIBLE else View.GONE
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
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

    private fun showCreateDialog() {
        val titleInput = EditText(requireContext()).apply {
            hint = getString(R.string.lists_title_hint)
            setPadding(48, 32, 48, 16)
        }
        val descInput = EditText(requireContext()).apply {
            hint = getString(R.string.lists_description_hint)
            setPadding(48, 16, 48, 16)
        }
        val publicCheck = CheckBox(requireContext()).apply {
            text = getString(R.string.lists_public_checkbox)
            setPadding(48, 8, 48, 32)
        }
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(titleInput)
            addView(descInput)
            addView(publicCheck)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.lists_create)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.lists_create) { _, _ ->
                val title = titleInput.text?.toString()?.trim().orEmpty()
                if (title.isEmpty()) {
                    Snackbar.make(binding.root, R.string.lists_title_required, Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.createList(title, descInput.text?.toString(), publicCheck.isChecked)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
