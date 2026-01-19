package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeHostFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home_host, container, false)

        val bottom = root.findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Show Feed by default
        childFragmentManager.beginTransaction()
            .replace(R.id.homeChildContainer, FeedFragment())
            .commit()

        bottom.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_feed -> childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, FeedFragment()).commit()
                R.id.nav_my_recipes -> childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, MyRecipesFragment()).commit()
                R.id.nav_manage_user -> childFragmentManager.beginTransaction().replace(R.id.homeChildContainer, ManageUserFragment()).commit()
            }
            true
        }

        return root
    }
}
