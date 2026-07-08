package com.example.trackrate.ui.moderation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemModerationBinding
import com.example.trackrate.domain.model.CatalogSubmission

class ModerationAdapter(
    private val onApprove: (CatalogSubmission) -> Unit,
    private val onReject: (CatalogSubmission) -> Unit
) : ListAdapter<CatalogSubmission, ModerationAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModerationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onApprove, onReject)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemModerationBinding,
        private val onApprove: (CatalogSubmission) -> Unit,
        private val onReject: (CatalogSubmission) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogSubmission) {
            val context = binding.root.context
            binding.title.text = item.title
            binding.typeIcon.setImageResource(item.type.iconRes())

            val subtitleParts = buildList {
                add(context.getString(item.type.labelRes()))
                item.subtitle?.let { add(it) }
            }
            binding.subtitle.text = subtitleParts.joinToString(" • ")

            binding.approveButton.setOnClickListener { onApprove(item) }
            binding.rejectButton.setOnClickListener { onReject(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CatalogSubmission>() {
            override fun areItemsTheSame(oldItem: CatalogSubmission, newItem: CatalogSubmission) =
                oldItem.type == newItem.type && oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CatalogSubmission, newItem: CatalogSubmission) =
                oldItem == newItem
        }
    }
}
