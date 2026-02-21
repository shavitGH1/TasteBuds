package com.sandg.tastebuds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.models.Ingredient
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import kotlinx.coroutines.launch

sealed class SaveRecipeState {
    object Idle : SaveRecipeState()
    object Loading : SaveRecipeState()
    object Success : SaveRecipeState()
    data class Error(val message: String) : SaveRecipeState()
}

class AddRecipeViewModel : ViewModel() {

    private val _saveState = MutableLiveData<SaveRecipeState>(SaveRecipeState.Idle)
    val saveState: LiveData<SaveRecipeState> = _saveState

    fun saveRecipe(
        existingId: String?,
        name: String,
        description: String?,
        imageUrl: String?,
        time: Int,
        difficulty: String,
        ingredients: List<Ingredient>,
        steps: List<String>,
        existingRecipe: Recipe?
    ) {
        _saveState.value = SaveRecipeState.Loading

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val recipeId = existingId
            ?: "${System.currentTimeMillis()}_${name.replace(" ", "_").lowercase()}"

        val recipe = Recipe(
            id = recipeId,
            name = name,
            imageUrlString = imageUrl?.takeIf { it.isNotBlank() },
            publisher = firebaseUser?.email,
            publisherId = firebaseUser?.uid,
            ingredients = ingredients,
            steps = steps,
            time = time,
            difficulty = difficulty,
            dietRestrictions = emptyList(),
            description = description?.takeIf { it.isNotBlank() },
            difficultyRating = existingRecipe?.difficultyRating,
            userRatings = existingRecipe?.userRatings ?: emptyMap()
        )

        viewModelScope.launch {
            try {
                Model.shared.addRecipe(recipe)
                _saveState.postValue(SaveRecipeState.Success)
            } catch (e: Exception) {
                _saveState.postValue(SaveRecipeState.Error(e.localizedMessage ?: "Save failed"))
            }
        }
    }
}

