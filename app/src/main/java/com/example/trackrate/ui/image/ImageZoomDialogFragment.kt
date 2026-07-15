package com.example.trackrate.ui.image

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import coil.load
import com.example.trackrate.R
import com.example.trackrate.databinding.DialogImageZoomBinding

class ImageZoomDialogFragment : DialogFragment() {

    private var _binding: DialogImageZoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.black)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogImageZoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUrl = requireArguments().getString(ARG_IMAGE_URL).orEmpty()
        val placeholderRes = requireArguments().getInt(ARG_PLACEHOLDER_RES, R.drawable.ic_mdi_album)

        binding.progress.visibility = View.VISIBLE
        binding.zoomImage.load(imageUrl) {
            placeholder(placeholderRes)
            error(placeholderRes)
            listener(
                onSuccess = { _, _ ->
                    binding.progress.visibility = View.GONE
                    binding.zoomImage.resetZoom()
                },
                onError = { _, _ ->
                    binding.progress.visibility = View.GONE
                }
            )
        }

        binding.zoomImage.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_PLACEHOLDER_RES = "placeholder_res"
        private const val TAG = "ImageZoomDialog"

        fun show(host: FragmentActivity, imageUrl: String, placeholderRes: Int = R.drawable.ic_mdi_album) {
            if (imageUrl.isBlank()) return
            if (host.supportFragmentManager.findFragmentByTag(TAG) != null) return
            ImageZoomDialogFragment().apply {
                arguments = bundleOf(
                    ARG_IMAGE_URL to imageUrl,
                    ARG_PLACEHOLDER_RES to placeholderRes
                )
            }.show(host.supportFragmentManager, TAG)
        }

        fun show(host: Fragment, imageUrl: String, placeholderRes: Int = R.drawable.ic_mdi_album) {
            show(host.requireActivity(), imageUrl, placeholderRes)
        }
    }
}
