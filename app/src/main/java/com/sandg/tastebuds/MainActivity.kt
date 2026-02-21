package com.sandg.tastebuds

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sandg.tastebuds.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null
    private var navController: NavController? = null

    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarNavIcon: ImageButton
    private lateinit var toolbarAddButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Set up the custom toolbar as the support action bar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(false)

        toolbarTitle = findViewById(R.id.toolbarTitle)
        toolbarNavIcon = findViewById(R.id.toolbarNavIcon)
        toolbarAddButton = findViewById(R.id.toolbarAddButton)

        // Nav icon navigates back
        toolbarNavIcon.setOnClickListener {
            navController?.popBackStack()
        }

        // Add button navigates to add recipe
        toolbarAddButton.setOnClickListener {
            navController?.navigate(R.id.action_global_addRecipeFragment)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            val params = bottomNav.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.bottomMargin = systemBars.bottom
            bottomNav.layoutParams = params

            WindowInsetsCompat.CONSUMED
        }

        bottomNav.setPadding(0, 0, 0, 0)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }

        setupTopBar()
    }

    override fun onResume() {
        super.onResume()
        try {
            val vm = ViewModelProvider(this)[SharedRecipesViewModel::class.java]
            vm.syncFavoritesForCurrentUser()
        } catch (_: Exception) {
        }
    }

    private fun setupTopBar() {
        val navHost = supportFragmentManager.findFragmentById(R.id.mainNavHost) as? NavHostFragment
        navController = navHost?.navController

        navController?.addOnDestinationChangedListener { controller, destination, arguments ->
            val label = when (destination.id) {
                R.id.homeHostFragment -> arguments?.getString("TITLE_KEY") ?: "Home"
                else -> destination.label?.toString() ?: ""
            }
            toolbarTitle.text = label

            // Show back arrow only when there's somewhere to go back to
            val canGoBack = controller.previousBackStackEntry != null
            toolbarNavIcon.visibility = if (canGoBack) View.VISIBLE else View.GONE

            // Show add button on all screens except the recipes list
            toolbarAddButton.visibility =
                if (destination.id == R.id.recipesListFragment) View.GONE else View.VISIBLE
        }
    }

    /** Public helper for child fragments to update the toolbar title. */
    fun setActionBarTitle(title: String) {
        toolbarTitle.text = title
    }
}