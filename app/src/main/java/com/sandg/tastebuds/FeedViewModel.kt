package com.sandg.tastebuds

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.models.Recipe

class FeedViewModel : ViewModel() {

    /** Filters the master list to exclude the current user's own recipes and applies search. */
    fun filterFeedRecipes(all: List<Recipe>, query: String): List<Recipe> {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        var list = all.filter { it.publisherId != currentUid }
        if (query.isNotEmpty()) {
            val q = query.lowercase()
            list = list.filter { recipe ->
                recipe.name.lowercase().contains(q) ||
                recipe.description?.lowercase()?.contains(q) == true ||
                recipe.difficulty?.lowercase()?.contains(q) == true ||
                recipe.ingredients.any { it.name.lowercase().contains(q) } ||
                recipe.steps.any { it.lowercase().contains(q) } ||
                recipe.time?.toString()?.contains(q) == true
            }
        }
        return list
    }
}

