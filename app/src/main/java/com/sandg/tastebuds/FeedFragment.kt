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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_feed, container, false)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)
        val swipe = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

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
        recycler.adapter = adapter

        // Load initial data from shared viewmodel
        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list.toList())
        }

        swipe.setOnRefreshListener {
            com.sandg.tastebuds.models.Model.shared.getAllRemoteRecipes { list ->
                sharedVm.setRecipes(list)
                swipe.isRefreshing = false
            }
        }

        return root
    }
}
