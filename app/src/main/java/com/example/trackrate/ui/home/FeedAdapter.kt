package com.example.trackrate.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.trackrate.R
import com.example.trackrate.databinding.ItemFeedBinding
import com.example.trackrate.domain.model.ActivityFeedItem
import com.example.trackrate.ui.moderation.iconRes
import com.example.trackrate.ui.social.activityText
import com.example.trackrate.ui.social.formatRelativeTime

class FeedAdapter(
    private val onProfileClick: (ActivityFeedItem) -> Unit,
    private val onEntityClick: (ActivityFeedItem) -> Unit
) : ListAdapter<ActivityFeedItem, FeedAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onProfileClick, onEntityClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    class ViewHolder(
        private val binding: ItemFeedBinding,
        private val onProfileClick: (ActivityFeedItem) -> Unit,
        private val onEntityClick: (ActivityFeedItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ActivityFeedItem) {
            val context = binding.root.context
            binding.userLabel.text = "@${item.username}"
            binding.activityLabel.text = item.activityText(context)
            binding.time.text = formatRelativeTime(item.createdAt)

            binding.avatar.load(item.avatarUrl) {
                placeholder(R.drawable.ic_mdi_account)
                error(R.drawable.ic_mdi_account)
                transformations(CircleCropTransformation())
            }

            binding.typeIcon.setImageResource(item.entityType.iconRes())
            binding.entityTitle.text = item.entityTitle
            binding.ratingBar.rating = item.rating.toFloat()

            if (!item.entitySubtitle.isNullOrBlank()) {
                binding.entitySubtitle.visibility = View.VISIBLE
                binding.entitySubtitle.text = item.entitySubtitle
            } else {
                binding.entitySubtitle.visibility = View.GONE
            }

            if (!item.review.isNullOrBlank()) {
                binding.review.visibility = View.VISIBLE
                binding.review.text = item.review
            } else {
                binding.review.visibility = View.GONE
            }

            binding.avatar.setOnClickListener { onProfileClick(item) }
            binding.userLabel.setOnClickListener { onProfileClick(item) }
            binding.activityLabel.setOnClickListener { onProfileClick(item) }
            binding.entityRow.setOnClickListener { onEntityClick(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ActivityFeedItem>() {
            override fun areItemsTheSame(oldItem: ActivityFeedItem, newItem: ActivityFeedItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ActivityFeedItem, newItem: ActivityFeedItem) =
                oldItem == newItem
        }
    }
}
