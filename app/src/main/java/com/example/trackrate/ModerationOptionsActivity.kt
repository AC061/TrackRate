package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.trackrate.ui.ThemedAppCompatActivity
import com.example.trackrate.databinding.ActivityModerationOptionsBinding
import com.example.trackrate.util.setBrandedTitle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ModerationOptionsActivity : ThemedAppCompatActivity() {

    private lateinit var binding: ActivityModerationOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModerationOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setBrandedTitle(R.string.moderation_options_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.moderationButton.setOnClickListener {
            startActivity(ModerationActivity.newIntent(this))
        }
        binding.adminUsersButton.setOnClickListener {
            startActivity(AdminUsersActivity.newIntent(this))
        }
        binding.labelsButton.setOnClickListener {
            startActivity(LabelsActivity.newIntent(this))
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, ModerationOptionsActivity::class.java)
    }
}
