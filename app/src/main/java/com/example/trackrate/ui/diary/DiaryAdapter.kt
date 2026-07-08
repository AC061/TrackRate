package com.example.trackrate.ui.diary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemDiaryBinding
import com.example.trackrate.domain.model.DiaryEntry
import com.example.trackrate.ui.moderation.iconRes

class DiaryAdapter(
    private val onClick: (DiaryEntry) -> Unit
) : ListAdapter<DiaryEntry, DiaryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemDiaryBinding,
        private val onClick: (DiaryEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DiaryEntry) {
            binding.title.text = item.title
            binding.typeIcon.setImageResource(item.entityType.iconRes())
            binding.ratingBar.rating = item.rating.toFloat()

            if (!item.subtitle.isNullOrBlank()) {
                binding.subtitle.visibility = View.VISIBLE
                binding.subtitle.text = item.subtitle
            } else {
                binding.subtitle.visibility = View.GONE
            }

            if (!item.review.isNullOrBlank()) {
                binding.review.visibility = View.VISIBLE
                binding.review.text = item.review
            } else {
                binding.review.visibility = View.GONE
            }

            binding.date.text = item.listenedAt.orEmpty()
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<DiaryEntry>() {
            override fun areItemsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DiaryEntry, newItem: DiaryEntry) =
                oldItem == newItem
        }
    }
}
