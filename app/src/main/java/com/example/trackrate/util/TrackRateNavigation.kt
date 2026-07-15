package com.example.trackrate.util

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.trackrate.R
import com.example.trackrate.domain.model.CatalogType

object TrackRateNavigation {

    fun navigateToProfile(fragment: Fragment, username: String) {
        fragment.findNavController().navigate(
            R.id.nav_profile,
            Bundle().apply { putString(ARG_USERNAME, username) }
        )
    }

    fun navigateToEditProfile(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_edit_profile)
    }

    fun navigateToSubmissions(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_submissions)
    }

    fun navigateToLists(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_lists)
    }

    fun navigateToListDetail(fragment: Fragment, listId: String, listTitle: String) {
        fragment.findNavController().navigate(
            R.id.nav_list_detail,
            Bundle().apply {
                putString(ARG_LIST_ID, listId)
                putString(ARG_LIST_TITLE, listTitle)
            }
        )
    }

    fun navigateToModerationOptions(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_moderation_options)
    }

    fun navigateToModeration(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_moderation)
    }

    fun navigateToModerationReview(fragment: Fragment, type: CatalogType, id: String) {
        fragment.findNavController().navigate(
            R.id.nav_moderation_review,
            Bundle().apply {
                putString(ARG_ENTITY_TYPE, type.name)
                putString(ARG_ENTITY_ID, id)
            }
        )
    }

    fun navigateToAdminUsers(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_admin_users)
    }

    fun navigateToLabels(fragment: Fragment) {
        fragment.findNavController().navigate(R.id.nav_labels)
    }

    const val ARG_USERNAME = "username"
    const val ARG_LIST_ID = "listId"
    const val ARG_LIST_TITLE = "listTitle"
    const val ARG_ENTITY_TYPE = "entityType"
    const val ARG_ENTITY_ID = "entityId"
}
