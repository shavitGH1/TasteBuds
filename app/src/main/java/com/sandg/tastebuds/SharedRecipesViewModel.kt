package com.sandg.tastebuds

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import com.sandg.tastebuds.dao.AppLocalDB
import android.os.Handler
import android.os.Looper

class SharedRecipesViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    companion object {
        private const val TAG = "SharedRecipesVM"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        reloadAll()
    }

    fun reloadAll(onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                val local = try {
                    AppLocalDB.db.recipeDao.getAllRecipes()
                } catch (_: Exception) { emptyList<Recipe>() }

                if (local.isNotEmpty()) {
                    _recipes.postValue(local)
                }

                Model.shared.getAllRemoteRecipes { remoteList ->
                    Thread {
                        try {
                            val map = mutableMapOf<String, Recipe>()
                            for (r in remoteList) {
                                map[r.id] = r
                            }
                            for (l in local) {
                                val existing = map[l.id]
                                if (existing != null) {
                                    map[l.id] = existing.copy(
                                        imageUrlString = existing.imageUrlString ?: l.imageUrlString,
                                        publisher = existing.publisher ?: l.publisher,
                                        publisherId = existing.publisherId ?: l.publisherId,
                                        ingredients = if (existing.ingredients.isNotEmpty()) existing.ingredients else l.ingredients,
                                        steps = if (existing.steps.isNotEmpty()) existing.steps else l.steps,
                                        time = existing.time ?: l.time,
                                        difficulty = existing.difficulty ?: l.difficulty,
                                        dietRestrictions = if (existing.dietRestrictions.isNotEmpty()) existing.dietRestrictions else l.dietRestrictions,
                                        description = existing.description ?: l.description
                                    )
                                } else {
                                    map[l.id] = l
                                }
                            }
                            _recipes.postValue(map.values.toList())
                        } catch (e: Exception) {
                            Log.e(TAG, "reloadAll: merge error", e)
                            _recipes.postValue(remoteList)
                        }
                        onComplete?.let { mainHandler.post(it) }
                    }.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "reloadAll: unexpected error", e)
                onComplete?.let { mainHandler.post(it) }
            }
        }.start()
    }

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
}
