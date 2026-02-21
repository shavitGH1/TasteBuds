package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sandg.tastebuds.models.Recipe

class MyRecipesFragment : BaseRecipeListFragment() {

    private val viewModel: MyRecipesViewModel by viewModels()
    private lateinit var adapter: RecipesAdapter
    private lateinit var swipe: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_my_recipes, container, false)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)
        swipe = root.findViewById(R.id.swipeRefresh)

        setupRecycler(recycler)
        setupSwipeRefresh()
        observeRecipes()

        return root
    }

    private fun setupRecycler(recycler: RecyclerView) {
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipesAdapter()
        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                saveLastTab(R.id.nav_my_recipes)
                findNavController().navigate(
                    R.id.action_global_recipeDetailFragment,
                    android.os.Bundle().apply { putString("recipeId", recipe.id) }
                )
            }
            override fun onRecipeOptions(recipe: Recipe, view: View) {
                showRecipeOptionsMenu(recipe, view)
            }
        }
        recycler.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        swipe.setOnRefreshListener {
            swipe.isRefreshing = true
            sharedVm.reloadAll { swipe.isRefreshing = false }
        }
    }

    private fun observeRecipes() {
        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            adapter.submitList(viewModel.filterMyRecipes(list))
        }
    }

    override fun onResume() {
        super.onResume()
        swipe.post {
            swipe.isRefreshing = true
            sharedVm.reloadAll { swipe.isRefreshing = false }
        }
    }
}
