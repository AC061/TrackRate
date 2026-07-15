package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.trackrate.ui.ThemedAppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.databinding.ActivitySubmissionsBinding
import com.example.trackrate.domain.model.CatalogSubmission
import com.example.trackrate.ui.moderation.SubmissionAdapter
import com.example.trackrate.ui.moderation.SubmissionsViewModel
import com.example.trackrate.util.setBrandedTitle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubmissionsActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivitySubmissionsBinding
    private val viewModel: SubmissionsViewModel by viewModels()
    private var pendingUploadItem: CatalogSubmission? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val item = pendingUploadItem
        if (uri != null && item != null) {
            viewModel.uploadImage(item, uri)
        }
        pendingUploadItem = null
    }

    private val adapter = SubmissionAdapter { item ->
        pendingUploadItem = item
        pickImage.launch("image/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setBrandedTitle(R.string.submissions_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility =
                        if (state.isLoading || state.isUploading) View.VISIBLE else View.GONE
                    adapter.submitList(state.items)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, SubmissionsActivity::class.java)
    }
}
