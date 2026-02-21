package com.sandg.tastebuds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import kotlinx.coroutines.launch

class RecipeDetailViewModel : ViewModel() {

    private val _recipe = MutableLiveData<Recipe?>()
    val recipe: LiveData<Recipe?> = _recipe

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun setRecipe(recipe: Recipe) { _recipe.value = recipe }

    fun loadRecipe(id: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = Model.shared.getRecipeById(id)
            _recipe.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun saveRating(recipe: Recipe, userId: String, rating: Int) {
        val updated = recipe.copy(
            userRatings = recipe.userRatings.toMutableMap().apply { put(userId, rating) }
        )
        _recipe.postValue(updated)
        viewModelScope.launch {
            Model.shared.addRecipe(updated)
        }
    }
}

