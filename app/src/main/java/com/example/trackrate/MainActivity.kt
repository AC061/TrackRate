package com.example.trackrate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.navigation.NavigationView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.trackrate.ui.ThemedAppCompatActivity
import com.example.trackrate.data.repository.AuthRepository
import com.example.trackrate.data.repository.PreferencesRepository
import com.example.trackrate.data.repository.ProfileRepository
import com.example.trackrate.databinding.ActivityMainBinding
import com.example.trackrate.util.TrackRateNavigation
import com.example.trackrate.util.setBrandedTitle
import com.example.trackrate.util.setFullLogo
import dagger.hilt.android.AndroidEntryPoint
import com.example.trackrate.domain.model.SessionStatus
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ThemedAppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var currentUsername: String? = null
    private var isCurrentUserAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesRepository.applyStoredTheme()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        observeSession()
        loadCurrentUsername()

        lifecycleScope.launch {
            authRepository.bootstrap()
        }

        binding.appBarMain.fab?.setOnClickListener {
            startActivity(SubmitActivity.newIntent(this))
        }

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        navController = navHostFragment.navController

        binding.navView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings
                ),
                binding.drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
        }

        binding.appBarMain.contentMain.bottomNavView?.let { bottomNav ->
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            bottomNav.setupWithNavController(navController)
            bottomNav.setOnItemReselectedListener { item ->
                navController.popBackStack(item.itemId, false)
            }
        }

        setupToolbarBranding(navController)
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    fun setToolbarBrandedTitle(title: CharSequence?) {
        binding.appBarMain.toolbar.setBrandedTitle(title)
    }

    private fun handleDeepLink(intent: Intent?) {
        when (intent?.getStringExtra(EXTRA_DESTINATION)) {
            DESTINATION_LISTS -> navController.navigate(R.id.nav_lists)
        }
    }

    private fun setupToolbarBranding(navController: NavController) {
        val toolbar = binding.appBarMain.toolbar
        val topLevelDestinations = setOf(
            R.id.nav_transform,
            R.id.nav_reflow,
            R.id.nav_slideshow,
            R.id.nav_settings
        )
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            binding.appBarMain.fab?.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE

            when (destination.id) {
                R.id.nav_transform -> toolbar.setFullLogo()
                else -> toolbar.setBrandedTitle(destination.label)
            }
        }
        navController.addOnDestinationChangedListener(listener)
        navController.currentDestination?.let { destination ->
            listener.onDestinationChanged(navController, destination, null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar, menu)
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            menuInflater.inflate(R.menu.overflow, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_moderation)?.isVisible = isCurrentUserAdmin
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_moderation -> {
                navController.navigate(R.id.nav_moderation_options)
                return true
            }
            R.id.action_profile -> {
                val username = currentUsername
                if (username != null) {
                    navController.navigate(
                        R.id.nav_profile,
                        Bundle().apply { putString(TrackRateNavigation.ARG_USERNAME, username) }
                    )
                }
                return true
            }
            R.id.nav_settings -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_settings)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        loadCurrentUsername()
    }

    private fun loadCurrentUsername() {
        lifecycleScope.launch {
            val profile = profileRepository.getCurrentProfile()
            val wasAdmin = isCurrentUserAdmin
            currentUsername = profile?.username
            isCurrentUserAdmin = profile?.isAdmin == true
            if (wasAdmin != isCurrentUserAdmin) {
                invalidateOptionsMenu()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun observeSession() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.sessionStatus.collect { status ->
                    when (status) {
                        is SessionStatus.Initializing -> Unit
                        is SessionStatus.NotAuthenticated -> {
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finish()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_DESTINATION = "extra_destination"
        private const val DESTINATION_LISTS = "lists"

        fun newIntentForLists(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_DESTINATION, DESTINATION_LISTS)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
    }
}
