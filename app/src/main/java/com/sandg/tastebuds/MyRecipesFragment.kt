package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MyRecipesFragment : Fragment() {
    private val sharedVm: SharedRecipesViewModel by activityViewModels()
    private lateinit var adapter: RecipesAdapter
    private lateinit var swipe: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_my_recipes, container, false)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)
        swipe = root.findViewById(R.id.swipeRefresh)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipesAdapter()

        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                // Save current tab before navigating away
                val prefs = requireContext().getSharedPreferences("home_host_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putInt("last_selected_tab", R.id.nav_my_recipes).apply()

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

        recycler.adapter = adapter

        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            // Only show recipes created by the current user (not liked recipes)
            val filtered = list.filter { r -> r.publisherId == currentUid }
            adapter.submitList(filtered)
        }

        swipe.setOnRefreshListener {
            swipe.isRefreshing = true
            sharedVm.reloadAll {
                swipe.isRefreshing = false
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
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

        // Hide edit/delete options if not the owner
        if (!isOwner) {
            popup.menu.findItem(R.id.action_delete_recipe)?.isVisible = false
            popup.menu.findItem(R.id.action_edit_recipe)?.isVisible = false
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
