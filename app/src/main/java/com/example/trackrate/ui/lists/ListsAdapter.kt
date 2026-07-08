package com.example.trackrate.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemListBinding
import com.example.trackrate.domain.model.MusicList

class ListsAdapter(
    private val onClick: (MusicList) -> Unit
) : ListAdapter<MusicList, ListsAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemListBinding,
        private val onClick: (MusicList) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MusicList) {
            binding.title.text = item.title
            if (!item.description.isNullOrBlank()) {
                binding.subtitle.visibility = View.VISIBLE
                binding.subtitle.text = item.description
            } else {
                binding.subtitle.visibility = View.GONE
            }
            binding.publicBadge.visibility = if (item.isPublic) View.VISIBLE else View.GONE
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<MusicList>() {
            override fun areItemsTheSame(oldItem: MusicList, newItem: MusicList) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: MusicList, newItem: MusicList) = oldItem == newItem
        }
    }
}
