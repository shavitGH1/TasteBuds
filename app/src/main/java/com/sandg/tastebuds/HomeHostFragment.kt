package com.sandg.tastebuds

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeHostFragment : Fragment() {

    companion object {
        private const val PREFS_NAME = "home_host_prefs"
        private const val KEY_LAST_SELECTED_TAB = "last_selected_tab"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home_host, container, false)

        val bottom = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val childContainer = root.findViewById<View>(R.id.homeChildContainer)

        // base nav height in dp (should match layout xml height)
        val baseNavDp = 88
        val density = resources.displayMetrics.density
        val baseNavPx = (baseNavDp * density).toInt()

        // Ensure the bottom nav and content reserve space for system navigation bar
        bottom?.let { nav ->
            ViewCompat.setOnApplyWindowInsetsListener(nav) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val navBarHeight = systemBars.bottom

                // Increase the view height by the navBar inset so internal layout doesn't clip
                val lp = nav.layoutParams
                if (lp != null) {
                    val desiredHeight = baseNavPx + navBarHeight
                    if (lp.height != desiredHeight) {
                        lp.height = desiredHeight
                        nav.layoutParams = lp
                    }
                }

                // Preserve child container's existing bottom padding and add nav inset
                val cLeft = childContainer.paddingLeft
                val cTop = childContainer.paddingTop
                val cRight = childContainer.paddingRight
                val cBottom = childContainer.paddingBottom
                childContainer.setPadding(cLeft, cTop, cRight, navBarHeight + cBottom)

                insets
            }

            // Remove any item background and prevent clipping so icons render fully
            nav.setItemBackgroundResource(0)
            nav.clipToPadding = false
            nav.clipChildren = false

            // Selected color: user's purple; Unselected: muted (semi-transparent) purple
            val selectedColor = ContextCompat.getColor(requireContext(), R.color.bottom_nav_icon_tint)
            val unselectedColor = (0x99 shl 24) or (selectedColor and 0x00FFFFFF)

            val states = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            )
            val colors = intArrayOf(selectedColor, unselectedColor)
            val tintList = ColorStateList(states, colors)
            nav.itemIconTintList = tintList

            // Use muted purple as ripple color to avoid white flash on press
            try {
                nav.itemRippleColor = ColorStateList.valueOf(unselectedColor)
            } catch (_: Exception) {
            }

            // Apply icon size in pixels (density-aware) to ensure it's not clipped
            val iconSizePx = (24 * density).toInt()
            nav.itemIconSize = iconSizePx

            // Re-assign the feed icon explicitly from our vector drawable to avoid cached PNGs
            try {
                val feedItem = nav.menu.findItem(R.id.nav_feed)
                feedItem?.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_earth)
            } catch (_: Exception) {
            }

            nav.setOnItemSelectedListener { item: MenuItem ->
                // Check if we're currently visible (not on RecipeDetail or other nav destinations)
                val navController = (activity as? MainActivity)?.let { act ->
                    val navHost = act.supportFragmentManager.findFragmentById(R.id.mainNavHost) as? androidx.navigation.fragment.NavHostFragment
                    navHost?.navController
                }

                val currentDestinationId = navController?.currentDestination?.id

                // If we're on RecipeDetail, AddRecipe, or MealSearch, save the tab and pop back first
                if (currentDestinationId == R.id.recipeDetailFragment ||
                    currentDestinationId == R.id.addRecipeFragment ||
                    currentDestinationId == R.id.mealSearchFragment) {

                    // Save the desired tab
                    when (item.itemId) {
                        R.id.nav_feed -> saveLastSelectedTab(R.id.nav_feed)
                        R.id.nav_my_recipes -> saveLastSelectedTab(R.id.nav_my_recipes)
                        R.id.nav_manage_user -> saveLastSelectedTab(R.id.nav_manage_user)
                    }

                    // Pop back to HomeHost - onResume will handle showing the right tab
                    navController?.popBackStack(R.id.homeHostFragment, false)
                    return@setOnItemSelectedListener true
                }

                // We're on HomeHost - handle normally
                when (item.itemId) {
                    R.id.nav_feed -> {
                        childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, FeedFragment()).commit()
                        (activity as? MainActivity)?.setActionBarTitle("Feed")
                        saveLastSelectedTab(R.id.nav_feed)
                    }
                    R.id.nav_my_recipes -> {
                        childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, MyRecipesFragment()).commit()
                        (activity as? MainActivity)?.setActionBarTitle("My Recipes")
                        saveLastSelectedTab(R.id.nav_my_recipes)
                    }
                    R.id.nav_manage_user -> {
                        childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, ManageUserFragment()).commit()
                        (activity as? MainActivity)?.setActionBarTitle("Profile")
                        saveLastSelectedTab(R.id.nav_manage_user)
                    }
                }
                true
            }
        }

        // Restore last selected tab or show Feed by default
        val lastTabId = getLastSelectedTab()
        when (lastTabId) {
            R.id.nav_my_recipes -> {
                childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, MyRecipesFragment()).commit()
                (activity as? MainActivity)?.setActionBarTitle("My Recipes")
                bottom?.selectedItemId = R.id.nav_my_recipes
            }
            R.id.nav_manage_user -> {
                childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, ManageUserFragment()).commit()
                (activity as? MainActivity)?.setActionBarTitle("Profile")
                bottom?.selectedItemId = R.id.nav_manage_user
            }
            else -> {
                // Default to Feed
                childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, FeedFragment()).commit()
                (activity as? MainActivity)?.setActionBarTitle("Feed")
                bottom?.selectedItemId = R.id.nav_feed
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()

        // Check if we need to switch tabs (e.g., when returning from RecipeDetail)
        val lastTabId = getLastSelectedTab()
        val bottom = activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)
        val currentSelectedId = bottom?.selectedItemId ?: R.id.nav_feed

        // If the saved tab doesn't match the current bottom nav selection, switch to it
        if (lastTabId != currentSelectedId) {
            bottom?.selectedItemId = lastTabId

            // Also manually switch the fragment in case the listener doesn't fire
            when (lastTabId) {
                R.id.nav_my_recipes -> {
                    childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, MyRecipesFragment()).commit()
                    (activity as? MainActivity)?.setActionBarTitle("My Recipes")
                }
                R.id.nav_manage_user -> {
                    childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, ManageUserFragment()).commit()
                    (activity as? MainActivity)?.setActionBarTitle("Profile")
                }
                R.id.nav_feed -> {
                    childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, FeedFragment()).commit()
                    (activity as? MainActivity)?.setActionBarTitle("Feed")
                }
            }
        }
    }

    private fun saveLastSelectedTab(tabId: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_SELECTED_TAB, tabId).apply()
    }

    private fun getLastSelectedTab(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_SELECTED_TAB, R.id.nav_feed)
    }
}
