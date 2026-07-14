package com.example.trackrate.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemContributorBinding
import com.example.trackrate.domain.model.CatalogContributor
import com.example.trackrate.domain.model.CatalogType

class ContributorAdapter(
    private val onArtistClick: (String) -> Unit
) : ListAdapter<CatalogContributor, ContributorAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContributorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onArtistClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemContributorBinding,
        private val onArtistClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogContributor) {
            binding.contributorTitle.text = "${item.roleLabel} — ${item.artistName}"
            if (item.notes.isNullOrBlank()) {
                binding.contributorNotes.visibility = View.GONE
            } else {
                binding.contributorNotes.visibility = View.VISIBLE
                binding.contributorNotes.text = item.notes
            }
            binding.contributorTitle.setOnClickListener {
                onArtistClick(item.artistId)
            }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CatalogContributor>() {
            override fun areItemsTheSame(oldItem: CatalogContributor, newItem: CatalogContributor): Boolean =
                oldItem.artistId == newItem.artistId && oldItem.role == newItem.role

            override fun areContentsTheSame(oldItem: CatalogContributor, newItem: CatalogContributor): Boolean =
                oldItem == newItem
        }
    }
}
