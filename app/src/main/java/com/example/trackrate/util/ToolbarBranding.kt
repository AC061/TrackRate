package com.example.trackrate.util

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.trackrate.R

object ToolbarBranding {

    fun applyFullLogo(toolbar: Toolbar) {
        val branding = ensureBrandingView(toolbar)
        branding.findViewById<View>(R.id.toolbar_logo_full).visibility = View.VISIBLE
        branding.findViewById<View>(R.id.toolbar_title_row).visibility = View.GONE
        applyTitleSuppression(toolbar)
    }

    fun applyIconWithTitle(toolbar: Toolbar, title: CharSequence?) {
        val branding = ensureBrandingView(toolbar)
        branding.findViewById<View>(R.id.toolbar_logo_full).visibility = View.GONE
        branding.findViewById<View>(R.id.toolbar_title_row).visibility = View.VISIBLE
        branding.findViewById<TextView>(R.id.toolbar_title_text).text = title
        applyTitleSuppression(toolbar)
    }

    private fun applyTitleSuppression(toolbar: Toolbar) {
        disableDefaultTitle(toolbar)
        toolbar.post { suppressStrayTitleViews(toolbar) }
    }

    private fun disableDefaultTitle(toolbar: Toolbar) {
        toolbar.title = null
        toolbar.subtitle = null
        (toolbar.context as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowCustomEnabled(true)
        }
    }

    private fun suppressStrayTitleViews(toolbar: Toolbar) {
        disableDefaultTitle(toolbar)
        for (index in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(index)
            if (child is TextView && !isBrandingView(child)) {
                child.visibility = View.GONE
            }
        }
    }

    private fun isBrandingView(view: View): Boolean {
        if (view.id == R.id.toolbar_title_text) return true
        var current: View? = view
        while (current != null) {
            if (current.id == R.id.toolbar_branding_root) return true
            current = current.parent as? View
        }
        return false
    }

    private fun ensureBrandingView(toolbar: Toolbar): View {
        findBrandingRoot(toolbar)?.let { return it }

        toolbar.findViewById<View>(R.id.toolbar_branding_root)?.let { legacy ->
            toolbar.removeView(legacy)
        }

        val branding = LayoutInflater.from(toolbar.context)
            .inflate(R.layout.view_toolbar_branding, toolbar, false)

        val activity = toolbar.context as? AppCompatActivity
        if (activity?.supportActionBar != null) {
            disableDefaultTitle(toolbar)
            activity.supportActionBar?.setCustomView(
                branding,
                ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER_VERTICAL or Gravity.START
                )
            )
        } else {
            val layoutParams = Toolbar.LayoutParams(
                Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            toolbar.addView(branding, layoutParams)
        }
        return branding
    }

    private fun findBrandingRoot(toolbar: Toolbar): View? {
        val activity = toolbar.context as? AppCompatActivity
        val customView = activity?.supportActionBar?.customView
        if (customView != null) {
            if (customView.id == R.id.toolbar_branding_root) return customView
            customView.findViewById<View>(R.id.toolbar_branding_root)?.let { return it }
        }
        return toolbar.findViewById(R.id.toolbar_branding_root)
    }
}

fun Toolbar.setFullLogo() {
    ToolbarBranding.applyFullLogo(this)
}

fun Toolbar.setBrandedTitle(title: CharSequence?) {
    ToolbarBranding.applyIconWithTitle(this, title)
}

fun Toolbar.setBrandedTitle(@StringRes titleRes: Int) {
    setBrandedTitle(context.getString(titleRes))
}
