package com.example.trackrate.ui.moderation

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.trackrate.databinding.ItemSubmissionBinding
import com.example.trackrate.domain.model.CatalogSubmission

class SubmissionAdapter(
    private val onUploadImage: (CatalogSubmission) -> Unit
) : ListAdapter<CatalogSubmission, SubmissionAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubmissionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, onUploadImage)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemSubmissionBinding,
        private val onUploadImage: (CatalogSubmission) -> Unit
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

            binding.statusIcon.setImageResource(item.status.iconRes())
            binding.statusLabel.setText(item.status.labelRes())

            if (!item.rejectionReason.isNullOrBlank()) {
                binding.rejectionReason.visibility = View.VISIBLE
                binding.rejectionReason.text = item.rejectionReason
            } else {
                binding.rejectionReason.visibility = View.GONE
            }

            binding.uploadButton.setOnClickListener { onUploadImage(item) }
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
