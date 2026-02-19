package com.sandg.tastebuds

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.sandg.tastebuds.models.FirebaseModel
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe

class FeedFragment : Fragment() {

    private val sharedVm: SharedRecipesViewModel by activityViewModels()
    private lateinit var adapter: GridRecipesAdapter
    private val firebaseModel = FirebaseModel()
    private lateinit var swipe: SwipeRefreshLayout
    private var showLikedOnly = false
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_feed, container, false)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)
        swipe = root.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        val chipShowLikedOnly = root.findViewById<com.google.android.material.chip.Chip>(R.id.chipShowLikedOnly)
        val searchEditText = root.findViewById<EditText>(R.id.searchEditText)

        // Handle search input
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                updateRecipesList()
            }
        })

        // Handle filter chip
        chipShowLikedOnly.setOnCheckedChangeListener { _, isChecked ->
            showLikedOnly = isChecked
            updateRecipesList()
        }

        val span = 2
        recycler.layoutManager = GridLayoutManager(requireContext(), span)
        adapter = GridRecipesAdapter()
        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                // Save current tab before navigating away
                val prefs = requireContext().getSharedPreferences("home_host_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putInt("last_selected_tab", R.id.nav_feed).apply()

                val args = bundleOf("recipeId" to recipe.id)
                findNavController().navigate(R.id.action_global_recipeDetailFragment, args)
            }

            override fun onToggleFavorite(recipe: Recipe) {
                sharedVm.toggleFavorite(recipe)
            }

            override fun onRecipeOptions(recipe: Recipe, view: View) {
                showRecipeOptionsMenu(recipe, view)
            }
        }
        // ensure adapter stable ids (adapter already sets this, but reinforce)
        adapter.setHasStableIds(true)
        recycler.adapter = adapter

        // Prevent content from being clipped by bottom nav
        recycler.clipToPadding = false

        // Load initial data from shared viewmodel
        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            updateRecipesList()
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

    private fun updateRecipesList() {
        val allRecipes = sharedVm.recipes.value ?: emptyList()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        // Filter out recipes created by current user
        var filteredList = allRecipes.filter { recipe ->
            recipe.publisherId != currentUid
        }

        // Apply liked filter
        if (showLikedOnly) {
            filteredList = filteredList.filter { it.isFavorite }
        }

        // Apply search filter across all fields
        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            filteredList = filteredList.filter { recipe ->
                recipe.name?.lowercase()?.contains(query) == true ||
                recipe.description?.lowercase()?.contains(query) == true ||
                recipe.difficulty?.lowercase()?.contains(query) == true ||
                recipe.ingredients.any { ing ->
                    ing.name.lowercase().contains(query) ||
                    ing.unit?.lowercase()?.contains(query) == true
                } ||
                recipe.steps.any { step -> step.lowercase().contains(query) } ||
                recipe.time?.toString()?.contains(query) == true
            }
        }

        adapter.submitList(filteredList.toList())
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

    private fun showRecipeOptionsMenu(recipe: Recipe, view: View) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = !currentUid.isNullOrEmpty() && !recipe.publisherId.isNullOrEmpty() && currentUid == recipe.publisherId

        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.recipe_options_menu, popup.menu)

        // Hide delete option if not the owner
        if (!isOwner) {
            popup.menu.findItem(R.id.action_delete_recipe)?.isVisible = false
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_recipe -> {
                    if (isOwner) {
                        editRecipe(recipe)
                    } else {
                        showStyledToast("You can only edit your own recipes")
                    }
                    true
                }
                R.id.action_delete_recipe -> {
                    if (isOwner) {
                        deleteRecipe(recipe)
                    } else {
                        showStyledToast("You can only delete your own recipes")
                    }
                    true
                }
                R.id.action_share_recipe -> {
                    shareRecipe(recipe)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun editRecipe(recipe: Recipe) {
        val bundle = Bundle().apply {
            putString("recipeId", recipe.id)
            putString("recipeName", recipe.name)
            putString("description", recipe.description)
            putInt("time", recipe.time ?: 30)
            putString("difficulty", recipe.difficulty)
            putString("imageUrl", recipe.imageUrlString)
            putStringArrayList("steps", ArrayList(recipe.steps))
            val ingredientsJson = Gson().toJson(recipe.ingredients)
            putString("ingredientsJson", ingredientsJson)
        }
        findNavController().navigate(R.id.action_global_addRecipeFragment, bundle)
    }

    private fun deleteRecipe(recipe: Recipe) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete \"${recipe.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                Model.shared.deleteRecipe(recipe)
                showStyledToast("Recipe deleted")
                sharedVm.reloadAll {}
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareRecipe(recipe: Recipe) {
        val shareText = buildString {
            append("Check out this recipe: ${recipe.name}\n\n")
            if (!recipe.description.isNullOrEmpty()) {
                append("${recipe.description}\n\n")
            }
            append("Ingredients:\n")
            recipe.ingredients.forEach { ing ->
                append("\u2022 ${ing.amount?.toString() ?: ""} ${ing.unit ?: ""} ${ing.name}\n")
            }
            append("\nSteps:\n")
            recipe.steps.forEachIndexed { idx, step ->
                append("${idx + 1}. $step\n")
            }
        }

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        startActivity(android.content.Intent.createChooser(intent, "Share Recipe"))
    }
}
