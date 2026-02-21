package com.sandg.tastebuds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import kotlinx.coroutines.launch

class SharedRecipesViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    init { reloadAll() }

    fun reloadAll(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val list = Model.shared.getAllRecipes()
            _recipes.postValue(list)
            onComplete?.invoke()
        }
    }

    fun setRecipes(list: List<Recipe>) { _recipes.postValue(list) }

    fun refreshRecipe(id: String) {
        viewModelScope.launch {
            val recipe = Model.shared.getRecipeById(id) ?: return@launch
            val current = _recipes.value ?: emptyList()
            val idx = current.indexOfFirst { it.id == id }
            val updated = if (idx >= 0)
                current.toMutableList().apply { this[idx] = recipe }
            else
                current + recipe
            _recipes.postValue(updated)
        }
    }
}
