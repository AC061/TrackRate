package com.example.trackrate.util

import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout

fun ViewGroup.stripAppBarFromCoordinator() {
    if (childCount == 0) return
    val first = getChildAt(0)
    if (first.javaClass.simpleName.contains("AppBarLayout", ignoreCase = true)) {
        removeView(first)
    }
}

fun View.stripAppBarFromCoordinatorRoot() {
    (this as? CoordinatorLayout)?.stripAppBarFromCoordinator()
        ?: (this as? ViewGroup)?.let { group ->
            if (group is CoordinatorLayout) {
                group.stripAppBarFromCoordinator()
            }
        }
}
