package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sandg.tastebuds.models.FirebaseModel
import com.sandg.tastebuds.models.Recipe

class FeedFragment : Fragment() {

    private val sharedVm: SharedRecipesViewModel by activityViewModels()
    private lateinit var adapter: GridRecipesAdapter
    private val firebaseModel = FirebaseModel()
    private lateinit var swipe: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_feed, container, false)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)
        swipe = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        val span = 2
        recycler.layoutManager = GridLayoutManager(requireContext(), span)
        adapter = GridRecipesAdapter()
        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                val args = bundleOf("recipeId" to recipe.id)
                findNavController().navigate(R.id.action_recipesListFragment_to_recipeDetailFragment, args)
            }

            override fun onToggleFavorite(recipe: Recipe) {
                sharedVm.toggleFavorite(recipe)
            }
        }
        // ensure adapter stable ids (adapter already sets this, but reinforce)
        adapter.setHasStableIds(true)
        recycler.adapter = adapter

        // Prevent content from being clipped by bottom nav
        recycler.clipToPadding = false

        // Load initial data from shared viewmodel
        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
        }

        swipe.setOnRefreshListener {
            // Use shared viewmodel to reload both local and remote recipes and merge them
            swipe.isRefreshing = true
            sharedVm.reloadAll {
                swipe.isRefreshing = false
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        // Automatically refresh the feed when the fragment becomes visible
        // show spinner while reloading
        swipe.post {
            swipe.isRefreshing = true
            sharedVm.reloadAll {
                swipe.isRefreshing = false
            }
        }
    }
}
