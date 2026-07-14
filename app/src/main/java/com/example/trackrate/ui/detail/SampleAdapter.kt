package com.example.trackrate.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemSampleBinding
import com.example.trackrate.domain.model.CatalogSample

class SampleAdapter(
    private val onTrackClick: (String) -> Unit
) : ListAdapter<CatalogSample, SampleAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSampleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onTrackClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemSampleBinding,
        private val onTrackClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogSample) {
            binding.sampleTitle.text = item.title
            val subtitleParts = buildList {
                add(item.artistName)
                item.albumTitle?.let { add(it) }
            }
            binding.sampleSubtitle.text = subtitleParts.joinToString(" • ")
            if (item.notes.isNullOrBlank()) {
                binding.sampleNotes.visibility = View.GONE
            } else {
                binding.sampleNotes.visibility = View.VISIBLE
                binding.sampleNotes.text = item.notes
            }
            binding.root.setOnClickListener { onTrackClick(item.trackId) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CatalogSample>() {
            override fun areItemsTheSame(oldItem: CatalogSample, newItem: CatalogSample): Boolean =
                oldItem.trackId == newItem.trackId

            override fun areContentsTheSame(oldItem: CatalogSample, newItem: CatalogSample): Boolean =
                oldItem == newItem
        }
    }
}
