package com.sandg.tastebuds

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import com.sandg.tastebuds.dao.AppLocalDB

class SharedRecipesViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    companion object {
        private const val TAG = "SharedRecipesVM"
    }

    init {
        reloadAll()
    }

    fun reloadAll(onComplete: (() -> Unit)? = null) {
        // 1) Load local recipes immediately so UI shows saved items on startup
        Thread {
            try {
                val local = try {
                    AppLocalDB.db.recipeDao.getAllRecipes()
                } catch (e: Exception) {
                    emptyList<Recipe>()
                }

                Log.d(TAG, "reloadAll: local count=${local.size} ids=${local.map { it.id }}")

                if (local.isNotEmpty()) {
                    _recipes.postValue(local)
                }

                // 2) Fetch remote recipes and merge with local (union)
                Model.shared.getAllRemoteRecipes { remoteList ->
                    Log.d(TAG, "reloadAll: remote count=${remoteList.size} ids=${remoteList.map { it.id }}")
                    try {
                        val map = mutableMapOf<String, Recipe>()
                        // start with remote
                        for (r in remoteList) map[r.id] = r
                        // merge local, preserving favorites
                        for (l in local) {
                            val existing = map[l.id]
                            if (existing != null) {
                                val merged = existing.copy(isFavorite = existing.isFavorite || l.isFavorite)
                                map[l.id] = merged
                            } else {
                                map[l.id] = l
                            }
                        }
                        val merged = map.values.toList()
                        Log.d(TAG, "reloadAll: merged count=${merged.size} ids=${merged.map { it.id }}")
                        _recipes.postValue(merged)
                    } catch (e: Exception) {
                        Log.e(TAG, "reloadAll: merge error", e)
                        // On error, post remote list fallback
                        _recipes.postValue(remoteList)
                    }

                    // notify caller that reload finished
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "reloadAll: unexpected error", e)
                // in case of unexpected error, try remote only
                Model.shared.getAllRemoteRecipes { remote ->
                    _recipes.postValue(remote)
                    onComplete?.invoke()
                }
            }
        }.start()
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

    // Accept already toggled recipe to avoid double-toggle issues from optimistic UI
    fun toggleFavorite(toggledRecipe: Recipe) {
        val current = _recipes.value ?: emptyList()
        val newList = current.toMutableList()
        val idx = newList.indexOfFirst { it.id == toggledRecipe.id }
        if (idx >= 0) newList[idx] = toggledRecipe else newList.add(toggledRecipe)
        _recipes.postValue(newList)

        Model.shared.addRecipe(toggledRecipe) {
            Model.shared.getRecipeById(toggledRecipe.id) { refreshed ->
                val curr = _recipes.value ?: emptyList()
                val mut = curr.toMutableList()
                val i = mut.indexOfFirst { it.id == refreshed.id }
                if (i >= 0) mut[i] = refreshed else mut.add(refreshed)
                _recipes.postValue(mut)
            }
        }
    }
}
