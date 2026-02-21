package com.sandg.tastebuds

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sandg.tastebuds.models.Recipe

class FeedFragment : BaseRecipeListFragment() {

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: GridRecipesAdapter
    private lateinit var swipe: SwipeRefreshLayout
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_feed, container, false)
        swipe = root.findViewById(R.id.swipeRefresh)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)
        val searchEditText = root.findViewById<EditText>(R.id.searchEditText)

        setupRecycler(recycler)
        setupSearch(searchEditText)
        setupSwipeRefresh()
        observeRecipes()

        return root
    }

    private fun setupRecycler(recycler: RecyclerView) {
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.clipToPadding = false
        adapter = GridRecipesAdapter()
        adapter.setHasStableIds(true)
        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                saveLastTab(R.id.nav_feed)
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

    private fun setupSearch(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                refreshList()
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipe.setOnRefreshListener {
            swipe.isRefreshing = true
            sharedVm.reloadAll { swipe.isRefreshing = false }
        }
    }

    private fun observeRecipes() {
        sharedVm.recipes.observe(viewLifecycleOwner) { refreshList() }
    }

    private fun refreshList() {
        val filtered = viewModel.filterFeedRecipes(sharedVm.recipes.value ?: emptyList(), searchQuery)
        adapter.submitList(filtered)
    }

    override fun onResume() {
        super.onResume()
        swipe.post {
            swipe.isRefreshing = true
            sharedVm.reloadAll { swipe.isRefreshing = false }
        }
    }
}
