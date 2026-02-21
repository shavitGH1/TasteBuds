package com.sandg.tastebuds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import kotlinx.coroutines.launch

class MyRecipesViewModel : ViewModel() {

    /** Filters the master list to only the current user's recipes. */
    fun filterMyRecipes(all: List<Recipe>): List<Recipe> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return all.filter { it.publisherId == uid }
    }

    /** Deletes a recipe; calls onDone on the main thread when finished. */
    fun deleteRecipe(recipe: Recipe, onDone: () -> Unit) {
        viewModelScope.launch {
            Model.shared.deleteRecipe(recipe)
            onDone()
        }
    }
}

