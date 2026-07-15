package com.example.trackrate.util

import android.view.View
import com.example.trackrate.databinding.ViewLoadErrorBinding

fun ViewLoadErrorBinding.bindLoadError(
    errorMessage: String?,
    showRetry: Boolean,
    onRetry: () -> Unit
) {
    if (errorMessage.isNullOrBlank()) {
        root.visibility = View.GONE
        return
    }
    root.visibility = View.VISIBLE
    errorText.text = errorMessage
    retryButton.visibility = if (showRetry) View.VISIBLE else View.GONE
    retryButton.setOnClickListener { onRetry() }
}
