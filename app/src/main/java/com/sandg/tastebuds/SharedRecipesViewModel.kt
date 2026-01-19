package com.sandg.tastebuds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe

class SharedRecipesViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    init {
        reloadAll()
    }

    fun reloadAll() {
        Model.shared.getAllRecipes { list ->
            _recipes.postValue(list)
        }
    }

    // Allow external callers (fragments) to replace the current recipes list
    fun setRecipes(list: List<Recipe>) {
        _recipes.postValue(list)
    }

    fun refreshRecipe(id: String) {
        Model.shared.getRecipeById(id) { recipe ->
            val current = _recipes.value ?: emptyList()
            val idx = current.indexOfFirst { it.id == id }
            val newList = if (idx >= 0) {
                current.toMutableList().apply { this[idx] = recipe }
            } else {
                current + recipe
            }
            _recipes.postValue(newList)
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        val toggled = recipe.copy(isFavorite = !recipe.isFavorite)

        val current = _recipes.value ?: emptyList()
        val newList = current.toMutableList()
        val idx = newList.indexOfFirst { it.id == toggled.id }
        if (idx >= 0) newList[idx] = toggled else newList.add(toggled)
        _recipes.postValue(newList)

        Model.shared.addRecipe(toggled) {
            Model.shared.getRecipeById(toggled.id) { refreshed ->
                val curr = _recipes.value ?: emptyList()
                val mut = curr.toMutableList()
                val i = mut.indexOfFirst { it.id == refreshed.id }
                if (i >= 0) mut[i] = refreshed else mut.add(refreshed)
                _recipes.postValue(mut)
            }
        }
    }
}
