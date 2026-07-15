package com.example.trackrate.ui.moderation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.trackrate.databinding.ActivityModerationOptionsBinding
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.stripAppBarFromCoordinatorRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ModerationOptionsFragment : Fragment() {

    private var _binding: ActivityModerationOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityModerationOptionsBinding.inflate(inflater, container, false)
        binding.root.stripAppBarFromCoordinatorRoot()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.moderationButton.setOnClickListener { TrackRateNavigation.navigateToModeration(this) }
        binding.adminUsersButton.setOnClickListener { TrackRateNavigation.navigateToAdminUsers(this) }
        binding.labelsButton.setOnClickListener { TrackRateNavigation.navigateToLabels(this) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
