package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.trackrate.databinding.ActivitySubmitBinding
import com.example.trackrate.databinding.ItemContributorDraftBinding
import com.example.trackrate.databinding.ItemSampleDraftBinding
import com.example.trackrate.domain.model.CatalogItem
import com.example.trackrate.domain.model.CatalogType
import com.example.trackrate.domain.model.ContributorDraft
import com.example.trackrate.domain.model.ContributorRole
import com.example.trackrate.domain.model.SampleDraft
import com.example.trackrate.ui.submit.SubmitViewModel
import com.example.trackrate.util.ImageUriReader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubmitActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubmitBinding
    private val viewModel: SubmitViewModel by viewModels()

    private var artists: List<CatalogItem> = emptyList()
    private var albums: List<CatalogItem> = emptyList()
    private var tracks: List<CatalogItem> = emptyList()
    private var selectedArtistId: String? = null
    private var selectedAlbumId: String? = null
    private var selectedImageUri: Uri? = null

    private var selectedLabelId: String? = null
    private var labels: List<com.example.trackrate.domain.model.RecordLabel> = emptyList()
    private val contributorRows = mutableListOf<ContributorRow>()
    private val sampleRows = mutableListOf<SampleRow>()

    private val roleLabels = ContributorRole.entries.map { it.label }.toTypedArray()
    private val roleValues = ContributorRole.entries.toTypedArray()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            binding.imagePreview.visibility = View.VISIBLE
            binding.imagePreview.load(uri)
        } else {
            binding.imagePreview.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.typeToggle.check(R.id.btn_type_artist)
        applyType(CatalogType.ARTIST)
        binding.typeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val type = when (checkedId) {
                R.id.btn_type_album -> CatalogType.ALBUM
                R.id.btn_type_track -> CatalogType.TRACK
                else -> CatalogType.ARTIST
            }
            viewModel.setType(type)
            applyType(type)
        }

        binding.artistDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedArtistId = artists.getOrNull(position)?.id
            selectedAlbumId = null
            binding.albumDropdown.setText("", false)
            selectedArtistId?.let { viewModel.onArtistSelected(it) }
        }

        binding.albumDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedAlbumId = albums.getOrNull(position)?.id
        }

        binding.labelDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedLabelId = labels.getOrNull(position)?.id
        }

        binding.pickImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.addContributorButton.setOnClickListener { addContributorRow() }
        binding.addSampleButton.setOnClickListener { addSampleRow() }
        binding.submitButton.setOnClickListener { submit() }

        observeState()
        observeSubmitted()
    }

    private fun applyType(type: CatalogType) {
        binding.bioLayout.visibility = if (type == CatalogType.ARTIST) View.VISIBLE else View.GONE
        binding.descriptionLayout.visibility =
            if (type == CatalogType.ALBUM || type == CatalogType.TRACK) View.VISIBLE else View.GONE
        binding.labelLayout.visibility =
            if (type == CatalogType.ALBUM || type == CatalogType.TRACK) View.VISIBLE else View.GONE
        binding.artistLayout.visibility = if (type == CatalogType.ARTIST) View.GONE else View.VISIBLE
        binding.yearLayout.visibility = if (type == CatalogType.ALBUM) View.VISIBLE else View.GONE
        binding.albumLayout.visibility = if (type == CatalogType.TRACK) View.VISIBLE else View.GONE
        binding.durationLayout.visibility = if (type == CatalogType.TRACK) View.VISIBLE else View.GONE

        val showContributors = type == CatalogType.ALBUM || type == CatalogType.TRACK
        binding.contributorsTitle.visibility = if (showContributors) View.VISIBLE else View.GONE
        binding.contributorsContainer.visibility = if (showContributors) View.VISIBLE else View.GONE
        binding.addContributorButton.visibility = if (showContributors) View.VISIBLE else View.GONE

        val showSamples = type == CatalogType.TRACK
        binding.samplesTitle.visibility = if (showSamples) View.VISIBLE else View.GONE
        binding.samplesContainer.visibility = if (showSamples) View.VISIBLE else View.GONE
        binding.addSampleButton.visibility = if (showSamples) View.VISIBLE else View.GONE

        if (!showContributors) clearContributorRows()
        if (!showSamples) clearSampleRows()

        binding.titleLayout.setHint(
            when (type) {
                CatalogType.ARTIST -> R.string.submit_name_hint
                CatalogType.ALBUM -> R.string.submit_album_title_hint
                CatalogType.TRACK -> R.string.submit_track_title_hint
            }
        )
    }

    private fun addContributorRow() {
        val rowBinding = ItemContributorDraftBinding.inflate(
            LayoutInflater.from(this), binding.contributorsContainer, true
        )
        rowBinding.contributorArtistDropdown.setSimpleItems(artists.map { it.title }.toTypedArray())
        rowBinding.contributorRoleDropdown.setSimpleItems(roleLabels)
        rowBinding.contributorRoleDropdown.setText(roleLabels.first(), false)

        val row = ContributorRow(rowBinding)
        rowBinding.contributorArtistDropdown.setOnItemClickListener { _, _, position, _ ->
            row.selectedArtistId = artists.getOrNull(position)?.id
        }
        rowBinding.contributorRoleDropdown.setOnItemClickListener { _, _, position, _ ->
            row.selectedRole = roleValues.getOrNull(position) ?: ContributorRole.PRODUCER
        }
        rowBinding.removeContributorButton.setOnClickListener {
            binding.contributorsContainer.removeView(rowBinding.root)
            contributorRows.remove(row)
        }
        contributorRows.add(row)
    }

    private fun addSampleRow() {
        val rowBinding = ItemSampleDraftBinding.inflate(
            LayoutInflater.from(this), binding.samplesContainer, true
        )
        rowBinding.sampleTrackDropdown.setSimpleItems(tracks.map { it.title }.toTypedArray())

        val row = SampleRow(rowBinding)
        rowBinding.sampleTrackDropdown.setOnItemClickListener { _, _, position, _ ->
            row.selectedTrackId = tracks.getOrNull(position)?.id
        }
        rowBinding.removeSampleButton.setOnClickListener {
            binding.samplesContainer.removeView(rowBinding.root)
            sampleRows.remove(row)
        }
        sampleRows.add(row)
    }

    private fun clearContributorRows() {
        binding.contributorsContainer.removeAllViews()
        contributorRows.clear()
    }

    private fun clearSampleRows() {
        binding.samplesContainer.removeAllViews()
        sampleRows.clear()
    }

    private fun collectContributors(): List<ContributorDraft> =
        contributorRows.mapNotNull { row ->
            val artistId = row.selectedArtistId ?: return@mapNotNull null
            ContributorDraft(
                artistId = artistId,
                role = row.selectedRole,
                notes = row.binding.contributorNotesInput.text?.toString()
            )
        }

    private fun collectSamples(): List<SampleDraft> =
        sampleRows.mapNotNull { row ->
            val trackId = row.selectedTrackId ?: return@mapNotNull null
            SampleDraft(
                sampledTrackId = trackId,
                notes = row.binding.sampleNotesInput.text?.toString()
            )
        }

    private fun submit() {
        val title = binding.titleInput.text?.toString().orEmpty()
        when (viewModel.uiState.value.type) {
            CatalogType.ARTIST -> viewModel.submitArtist(
                name = title,
                bio = binding.bioInput.text?.toString().orEmpty()
            )

            CatalogType.ALBUM -> viewModel.submitAlbum(
                title = title,
                artistId = selectedArtistId,
                releaseYear = binding.yearInput.text?.toString()?.toIntOrNull(),
                description = binding.descriptionInput.text?.toString(),
                labelId = selectedLabelId,
                contributors = collectContributors()
            )

            CatalogType.TRACK -> {
                val seconds = binding.durationInput.text?.toString()?.toIntOrNull()
                viewModel.submitTrack(
                    title = title,
                    artistId = selectedArtistId,
                    albumId = selectedAlbumId,
                    durationMs = seconds?.let { it * 1000 },
                    description = binding.descriptionInput.text?.toString(),
                    labelId = selectedLabelId,
                    contributors = collectContributors(),
                    samples = collectSamples()
                )
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.submitButton.isEnabled = !state.isSubmitting
                    binding.progress.visibility =
                        if (state.isSubmitting) View.VISIBLE else View.GONE

                    if (state.artists !== artists) {
                        artists = state.artists
                        binding.artistDropdown.setSimpleItems(artists.map { it.title }.toTypedArray())
                        contributorRows.forEach { row ->
                            row.binding.contributorArtistDropdown.setSimpleItems(
                                artists.map { it.title }.toTypedArray()
                            )
                        }
                    }
                    if (state.albums !== albums) {
                        albums = state.albums
                        binding.albumDropdown.setSimpleItems(albums.map { it.title }.toTypedArray())
                    }
                    if (state.tracks !== tracks) {
                        tracks = state.tracks
                        sampleRows.forEach { row ->
                            row.binding.sampleTrackDropdown.setSimpleItems(
                                tracks.map { it.title }.toTypedArray()
                            )
                        }
                    }
                    if (state.labels !== labels) {
                        labels = state.labels
                        binding.labelDropdown.setSimpleItems(labels.map { it.name }.toTypedArray())
                    }
                    binding.albumLayout.helperText =
                        if (state.type == CatalogType.TRACK && albums.isEmpty())
                            getString(R.string.submit_album_optional)
                        else null

                    state.message?.let {
                        Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    private fun observeSubmitted() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitted.collect { result ->
                    val imageUri = selectedImageUri
                    if (imageUri != null) {
                        try {
                            val payload = ImageUriReader.read(this@SubmitActivity, imageUri)
                            viewModel.uploadImage(result.type, result.entityId, payload)
                            Snackbar.make(
                                binding.root,
                                R.string.upload_image_success,
                                Snackbar.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Snackbar.make(
                                binding.root,
                                e.message ?: getString(R.string.submit_success),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Snackbar.make(binding.root, R.string.submit_success, Snackbar.LENGTH_LONG).show()
                    }
                    binding.root.postDelayed({ finish() }, 1200)
                }
            }
        }
    }

    private class ContributorRow(
        val binding: ItemContributorDraftBinding,
        var selectedArtistId: String? = null,
        var selectedRole: ContributorRole = ContributorRole.PRODUCER
    )

    private class SampleRow(
        val binding: ItemSampleDraftBinding,
        var selectedTrackId: String? = null
    )

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, SubmitActivity::class.java)
    }
}
