package com.example.trackrate.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.trackrate.DetailActivity
import com.example.trackrate.MainActivity
import com.example.trackrate.R
import com.example.trackrate.databinding.ActivityProfileBinding
import com.example.trackrate.domain.model.ProfileStats
import com.example.trackrate.domain.model.UserProfile
import com.example.trackrate.domain.model.UserRatingStats
import com.example.trackrate.ui.diary.DiaryAdapter
import com.example.trackrate.ui.image.ImageZoomDialogFragment
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val ratingsAdapter = DiaryAdapter { entry ->
        startActivity(DetailActivity.newIntent(requireContext(), entry.entityType, entry.entityId))
    }
    private var currentAvatarUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProfileBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ratingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.ratingsRecycler.adapter = ratingsAdapter

        binding.followButton.setOnClickListener { viewModel.toggleFollow() }
        binding.listsButton.setOnClickListener { TrackRateNavigation.navigateToLists(this) }
        binding.editProfileButton.setOnClickListener { TrackRateNavigation.navigateToEditProfile(this) }
        binding.submissionsButton.setOnClickListener { TrackRateNavigation.navigateToSubmissions(this) }
        binding.signOutButton.setOnClickListener { viewModel.signOut() }
        binding.avatar.setOnClickListener {
            currentAvatarUrl?.let { url ->
                ImageZoomDialogFragment.show(this, url, R.drawable.ic_mdi_account)
            }
        }

        val username = arguments?.getString(TrackRateNavigation.ARG_USERNAME).orEmpty()
        observeState()
        viewModel.load(username)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.content.visibility =
                        if (state.profile != null && !state.isLoading) View.VISIBLE else View.GONE

                    state.profile?.let { bindProfile(it, state.stats, state.ratingStats) }
                    ratingsAdapter.submitList(state.ratings)
                    binding.ratingsEmpty.visibility =
                        if (state.ratings.isEmpty() && state.profile != null) View.VISIBLE else View.GONE

                    binding.followButton.visibility =
                        if (state.isOwnProfile) View.GONE else View.VISIBLE
                    binding.listsButton.visibility =
                        if (state.isOwnProfile) View.VISIBLE else View.GONE
                    binding.ownProfileActions.visibility =
                        if (state.isOwnProfile) View.VISIBLE else View.GONE
                    binding.followButton.isEnabled = !state.isWorking
                    if (!state.isOwnProfile) {
                        if (state.isFollowing) {
                            binding.followButton.setText(R.string.profile_unfollow)
                            binding.followButton.setIconResource(R.drawable.ic_mdi_account_minus)
                        } else {
                            binding.followButton.setText(R.string.profile_follow)
                            binding.followButton.setIconResource(R.drawable.ic_mdi_account_plus)
                        }
                    }

                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun bindProfile(
        profile: UserProfile,
        stats: ProfileStats?,
        ratingStats: UserRatingStats?
    ) {
        (requireActivity() as? MainActivity)?.setToolbarBrandedTitle("@${profile.username}")
        binding.username.text = "@${profile.username}"
        binding.displayName.text = profile.fullName() ?: profile.displayName ?: profile.username
        binding.displayName.visibility =
            if (profile.fullName().isNullOrBlank() && profile.displayName.isNullOrBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.avatar.load(profile.avatarUrl) {
            placeholder(R.drawable.ic_mdi_account)
            error(R.drawable.ic_mdi_account)
            transformations(CircleCropTransformation())
        }
        currentAvatarUrl = profile.avatarUrl
        binding.avatar.contentDescription = getString(R.string.image_zoom_hint)

        binding.adminBadge.visibility = if (profile.isAdmin) View.VISIBLE else View.GONE

        if (!profile.bio.isNullOrBlank()) {
            binding.bio.visibility = View.VISIBLE
            binding.bio.text = profile.bio
        } else {
            binding.bio.visibility = View.GONE
        }

        binding.followerCount.text = (stats?.followerCount ?: 0).toString()
        binding.followingCount.text = (stats?.followingCount ?: 0).toString()
        binding.ratingCount.text = (stats?.ratingCount ?: 0).toString()

        if (ratingStats != null && ratingStats.totalRatings > 0) {
            binding.ratingStatsCard.visibility = View.VISIBLE
            binding.averageRating.text = getString(
                R.string.profile_average_rating,
                ratingStats.averageRating
            )
            binding.reviewCount.text = resources.getQuantityString(
                R.plurals.profile_review_count,
                ratingStats.reviewCount,
                ratingStats.reviewCount
            )
        } else {
            binding.ratingStatsCard.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
