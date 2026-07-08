package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trackrate.databinding.ActivityListsBinding
import com.example.trackrate.ui.lists.ListsAdapter
import com.example.trackrate.ui.lists.ListsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListsBinding
    private val viewModel: ListsViewModel by viewModels()
    private val adapter = ListsAdapter { list ->
        startActivity(ListDetailActivity.newIntent(this, list.id, list.title))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
        binding.fab.setOnClickListener { showCreateDialog() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    adapter.submitList(state.lists)
                    binding.emptyView.visibility =
                        if (!state.isLoading && state.lists.isEmpty()) View.VISIBLE else View.GONE
                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
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

    private fun showCreateDialog() {
        val titleInput = EditText(this).apply {
            hint = getString(R.string.lists_title_hint)
            setPadding(48, 32, 48, 16)
        }
        val descInput = EditText(this).apply {
            hint = getString(R.string.lists_description_hint)
            setPadding(48, 16, 48, 16)
        }
        val publicCheck = CheckBox(this).apply {
            text = getString(R.string.lists_public_checkbox)
            setPadding(48, 8, 48, 32)
        }
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(titleInput)
            addView(descInput)
            addView(publicCheck)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lists_create)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.lists_create) { _, _ ->
                val title = titleInput.text?.toString()?.trim().orEmpty()
                if (title.isEmpty()) {
                    Snackbar.make(binding.root, R.string.lists_title_required, Snackbar.LENGTH_SHORT).show()
                } else {
                    viewModel.createList(title, descInput.text?.toString(), publicCheck.isChecked)
                }
            }
            .show()
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, ListsActivity::class.java)
    }
}
