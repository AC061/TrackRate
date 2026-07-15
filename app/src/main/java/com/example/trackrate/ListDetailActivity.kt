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
import coil.load
import com.example.trackrate.databinding.ActivityListDetailBinding
import com.example.trackrate.ui.lists.ListDetailViewModel
import com.example.trackrate.ui.lists.ListItemsAdapter
import com.google.android.material.snackbar.Snackbar
import com.example.trackrate.util.setBrandedTitle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListDetailActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivityListDetailBinding
    private val viewModel: ListDetailViewModel by viewModels()
    private val adapter = ListItemsAdapter { item ->
        startActivity(DetailActivity.newIntent(this, item.entityType, item.entityId))
    }

    private val pickCover = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.uploadCover(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.uploadCoverButton.setOnClickListener {
            pickCover.launch("image/*")
        }

        val listId = intent.getStringExtra(EXTRA_LIST_ID).orEmpty()
        val listTitle = intent.getStringExtra(EXTRA_LIST_TITLE).orEmpty()
        binding.toolbar.setBrandedTitle(listTitle)
        viewModel.init(listId, listTitle)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.toolbar.setBrandedTitle(state.listTitle)
                    binding.progress.visibility = if (state.isLoading || state.isUploadingCover) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                    adapter.submitList(state.items)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE

                    if (state.coverUrl != null) {
                        binding.cover.visibility = View.VISIBLE
                        binding.cover.load(state.coverUrl)
                    }

                    binding.uploadCoverButton.isEnabled = !state.isUploadingCover

                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_LIST_ID = "extra_list_id"
        private const val EXTRA_LIST_TITLE = "extra_list_title"

        fun newIntent(context: Context, listId: String, listTitle: String): Intent =
            Intent(context, ListDetailActivity::class.java).apply {
                putExtra(EXTRA_LIST_ID, listId)
                putExtra(EXTRA_LIST_TITLE, listTitle)
            }
    }
}
