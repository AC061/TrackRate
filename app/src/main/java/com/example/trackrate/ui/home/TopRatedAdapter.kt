package com.example.trackrate.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.trackrate.R
import com.example.trackrate.databinding.ItemTopRatedTrackBinding
import com.example.trackrate.domain.model.TopRatedTrack

class TopRatedAdapter(
    private val onClick: (TopRatedTrack) -> Unit
) : ListAdapter<TopRatedTrack, TopRatedAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopRatedTrackBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemTopRatedTrackBinding,
        private val onClick: (TopRatedTrack) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopRatedTrack) {
            binding.title.text = item.title
            binding.subtitle.text = item.subtitle.orEmpty()
            binding.subtitle.visibility =
                if (item.subtitle.isNullOrBlank()) android.view.View.GONE
                else android.view.View.VISIBLE
            binding.rating.text = String.format(
                "%.1f (%d)",
                item.averageRating,
                item.ratingCount
            )
            binding.cover.load(item.imageUrl) {
                placeholder(R.drawable.ic_mdi_music_note)
                error(R.drawable.ic_mdi_music_note)
            }
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<TopRatedTrack>() {
            override fun areItemsTheSame(oldItem: TopRatedTrack, newItem: TopRatedTrack) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TopRatedTrack, newItem: TopRatedTrack) =
                oldItem == newItem
        }
    }
}
