package com.sandg.tastebuds

import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.sandg.tastebuds.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply only top inset here — fragments/layouts will manage bottom space (e.g., bottom nav)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        setupTopBar()
    }

    private fun setupTopBar() {
        val navHost = supportFragmentManager.findFragmentById(R.id.mainNavHost) as? NavHostFragment
        navController = navHost?.navController
        navController?.let {
            NavigationUI.setupActionBarWithNavController(this, it)
        }

        // Update centered title on destination changes
        navController?.addOnDestinationChangedListener { _, destination, arguments ->
            // If inside the HomeHostFragment, the visible child determines the title.
            val label = when (destination.id) {
                R.id.homeHostFragment -> {
                    // Try to get the child title from arguments (child fragment may pass TITLE_KEY)
                    arguments?.getString("TITLE_KEY") ?: "Home"
                }
                else -> destination.label?.toString() ?: ""
            }

            supportActionBar?.let { actionBar ->
                actionBar.setDisplayShowCustomEnabled(true)
                actionBar.setDisplayShowTitleEnabled(false)

                val titleTextView = TextView(this).apply {
                    text = label
                    textSize = 20f
                    setTextColor(getColor(android.R.color.black))
                    gravity = Gravity.CENTER
                    layoutParams = Toolbar.LayoutParams(
                        Toolbar.LayoutParams.MATCH_PARENT,
                        Toolbar.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                }
                actionBar.customView = titleTextView
            }
        }
    }

    // Public helper for child fragments to set the title when needed
    fun setActionBarTitle(title: String) {
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayShowCustomEnabled(true)
            actionBar.setDisplayShowTitleEnabled(false)
            val titleTextView = TextView(this).apply {
                text = title
                textSize = 20f
                setTextColor(getColor(android.R.color.black))
                gravity = Gravity.CENTER
                layoutParams = Toolbar.LayoutParams(
                    Toolbar.LayoutParams.MATCH_PARENT,
                    Toolbar.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.CENTER }
            }
            actionBar.customView = titleTextView
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val addItem = menu?.findItem(R.id.main_menu_add)
        val currentId = navController?.currentDestination?.id
        if (currentId == R.id.recipesListFragment) {
            addItem?.isVisible = false
        } else {
            addItem?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navController?.popBackStack()
                true
            }
            R.id.main_menu_add -> {
                navController?.navigate(R.id.action_global_addRecipeFragment)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}