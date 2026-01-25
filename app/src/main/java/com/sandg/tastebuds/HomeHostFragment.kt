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
                when (item.itemId) {
                    R.id.nav_feed -> {
                        childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, FeedFragment()).commit()
                        (activity as? MainActivity)?.setActionBarTitle("Feed")
                    }
                    R.id.nav_my_recipes -> {
                        childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, MyRecipesFragment()).commit()
                        (activity as? MainActivity)?.setActionBarTitle("My Recipes")
                    }
                    R.id.nav_manage_user -> {
                        childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, ManageUserFragment()).commit()
                        (activity as? MainActivity)?.setActionBarTitle("Profile")
                    }
                }
                true
            }
        }

        // Show Feed by default
        childFragmentManager.beginTransaction()
            .replace(R.id.homeChildContainer, FeedFragment())
            .commit()

        (activity as? MainActivity)?.setActionBarTitle("Feed")

        return root
    }
}
