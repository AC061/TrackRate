package com.example.trackrate.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemSubmissionBinding
import com.example.trackrate.domain.model.ListItemDetail
import com.example.trackrate.ui.moderation.iconRes
import com.example.trackrate.ui.moderation.labelRes

class ListItemsAdapter(
    private val onClick: (ListItemDetail) -> Unit
) : ListAdapter<ListItemDetail, ListItemsAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubmissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemSubmissionBinding,
        private val onClick: (ListItemDetail) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListItemDetail) {
            val context = binding.root.context
            binding.title.text = item.title
            binding.typeIcon.setImageResource(item.entityType.iconRes())
            binding.statusIcon.visibility = View.GONE
            binding.statusLabel.visibility = View.GONE
            binding.rejectionReason.visibility = View.GONE

            val subtitleParts = buildList {
                add(context.getString(item.entityType.labelRes()))
                item.subtitle?.let { add(it) }
            }
            binding.subtitle.text = subtitleParts.joinToString(" • ")
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ListItemDetail>() {
            override fun areItemsTheSame(oldItem: ListItemDetail, newItem: ListItemDetail) =
                oldItem.listId == newItem.listId &&
                    oldItem.entityType == newItem.entityType &&
                    oldItem.entityId == newItem.entityId

            override fun areContentsTheSame(oldItem: ListItemDetail, newItem: ListItemDetail) =
                oldItem == newItem
        }
    }
}
