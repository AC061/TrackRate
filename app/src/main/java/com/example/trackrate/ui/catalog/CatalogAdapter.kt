package com.example.trackrate.ui.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.trackrate.R
import com.example.trackrate.databinding.ItemCatalogBinding
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType

class CatalogAdapter(
    private val onClick: (CatalogItem) -> Unit
) : ListAdapter<CatalogItem, CatalogAdapter.CatalogViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogViewHolder {
        val binding = ItemCatalogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CatalogViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: CatalogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CatalogViewHolder(
        private val binding: ItemCatalogBinding,
        private val onClick: (CatalogItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogItem) {
            binding.title.text = item.title
            binding.subtitle.text = buildSubtitle(item)
            binding.subtitle.visibility =
                if (binding.subtitle.text.isNullOrBlank()) android.view.View.GONE
                else android.view.View.VISIBLE

            val placeholder = when (item.type) {
                CatalogType.ARTIST -> R.drawable.ic_mdi_account_music
                CatalogType.ALBUM -> R.drawable.ic_mdi_album
                CatalogType.TRACK -> R.drawable.ic_mdi_music_note
            }
            binding.typeIcon.setImageResource(placeholder)
            binding.cover.load(item.imageUrl) {
                placeholder(placeholder)
                error(placeholder)
            }

            binding.root.setOnClickListener { onClick(item) }
        }

        private fun buildSubtitle(item: CatalogItem): String {
            val parts = mutableListOf<String>()
            item.subtitle?.let { parts += it }
            item.year?.let { parts += it.toString() }
            return parts.joinToString(" • ")
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CatalogItem>() {
            override fun areItemsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean =
                oldItem.type == newItem.type && oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CatalogItem, newItem: CatalogItem): Boolean =
                oldItem == newItem
        }
    }
}
