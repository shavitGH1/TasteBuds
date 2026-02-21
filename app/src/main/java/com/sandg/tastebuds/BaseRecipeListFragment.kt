package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Base fragment that provides shared recipe actions (edit, delete, share, options menu)
 * and tab-tracking to avoid duplication across FeedFragment and MyRecipesFragment.
 */
abstract class BaseRecipeListFragment : Fragment() {

    protected val sharedVm: SharedRecipesViewModel by activityViewModels()

    // ── Tab tracking ──────────────────────────────────────────────────────────

    protected fun saveLastTab(tabId: Int) {
        requireContext()
            .getSharedPreferences("home_host_prefs", android.content.Context.MODE_PRIVATE)
            .edit { putInt("last_selected_tab", tabId) }
    }

    // ── Options menu ──────────────────────────────────────────────────────────

    protected fun showRecipeOptionsMenu(recipe: Recipe, anchor: View) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = !uid.isNullOrEmpty() && uid == recipe.publisherId

        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.recipe_options_menu, popup.menu)
        popup.menu.findItem(R.id.action_edit_recipe)?.isVisible = isOwner
        popup.menu.findItem(R.id.action_delete_recipe)?.isVisible = isOwner

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit_recipe -> { if (isOwner) navigateToEdit(recipe); true }
                R.id.action_delete_recipe -> { if (isOwner) confirmDeleteRecipe(recipe); true }
                R.id.action_share_recipe -> { shareRecipe(recipe); true }
                else -> false
            }
        }
        popup.show()
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    protected fun navigateToEdit(recipe: Recipe) {
        val bundle = Bundle().apply {
            putString("recipeId", recipe.id)
            putString("recipeName", recipe.name)
            putString("description", recipe.description)
            putInt("time", recipe.time ?: 30)
            putString("difficulty", recipe.difficulty)
            putString("imageUrl", recipe.imageUrlString)
            putStringArrayList("steps", ArrayList(recipe.steps))
            putString("ingredientsJson", Gson().toJson(recipe.ingredients))
        }
        findNavController().navigate(R.id.action_global_addRecipeFragment, bundle)
    }

    private fun confirmDeleteRecipe(recipe: Recipe) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete \"${recipe.name}\"?")
            .setPositiveButton("Delete") { _, _ -> deleteRecipe(recipe) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteRecipe(recipe: Recipe) {
        sharedVm.viewModelScope.launch {
            Model.shared.deleteRecipe(recipe)
            sharedVm.reloadAll()
        }
        showStyledToast("Recipe deleted")
    }

    protected fun shareRecipe(recipe: Recipe) {
        val text = buildString {
            append("Check out this recipe: ${recipe.name}\n\n")
            recipe.description?.let { append("$it\n\n") }
            append("Ingredients:\n")
            recipe.ingredients.forEach { append("• ${it.amount ?: ""} ${it.unit ?: ""} ${it.name}\n") }
            append("\nSteps:\n")
            recipe.steps.forEachIndexed { i, s -> append("${i + 1}. $s\n") }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share Recipe"))
    }
}

