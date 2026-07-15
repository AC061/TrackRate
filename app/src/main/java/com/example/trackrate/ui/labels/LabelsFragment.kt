package com.example.trackrate.ui.labels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.R
import com.example.trackrate.databinding.ActivityLabelsBinding
import com.example.trackrate.domain.model.RecordLabel
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LabelsFragment : Fragment() {

    private var _binding: ActivityLabelsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LabelsViewModel by viewModels()
    private val adapter = LabelAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLabelsBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.addLabelButton.setOnClickListener { showAddDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.labels)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.labels.isEmpty()) View.VISIBLE else View.GONE
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun showAddDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.labels_name_hint)
            setPadding(48, 32, 48, 32)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.labels_add)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.settings_save) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) viewModel.createLabel(name)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class LabelAdapter : ListAdapter<RecordLabel, LabelAdapter.ViewHolder>(DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_label, parent, false)
            return ViewHolder(view as TextView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        class ViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
            fun bind(item: RecordLabel) {
                textView.text = item.name
            }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<RecordLabel>() {
                override fun areItemsTheSame(oldItem: RecordLabel, newItem: RecordLabel) =
                    oldItem.id == newItem.id

                override fun areContentsTheSame(oldItem: RecordLabel, newItem: RecordLabel) =
                    oldItem == newItem
            }
        }
    }
}
