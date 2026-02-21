package com.sandg.tastebuds.models

import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.dao.AppLocalDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Model private constructor() {

    private val firebaseModel = FirebaseModel()

    companion object {
        val shared = Model()
    }

    /** Returns cached recipes immediately; fetches remote and caches when signed in. */
    suspend fun getAllRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        val local: List<Recipe> = runCatching { AppLocalDB.db.recipeDao.getAllRecipes() }.getOrElse { emptyList() }

        if (FirebaseAuth.getInstance().currentUser == null) return@withContext local

        val remote: List<Recipe> = runCatching { firebaseModel.getAllRecipesSync() }.getOrElse { emptyList() }
        if (remote.isNotEmpty()) {
            runCatching { AppLocalDB.db.recipeDao.insertRecipes(*remote.toTypedArray()) }
        }
        if (remote.isNotEmpty()) remote else local
    }

    suspend fun getAllRemoteRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        runCatching { firebaseModel.getAllRecipesSync() }.getOrElse { emptyList() }
    }

    suspend fun addRecipe(recipe: Recipe): Unit = withContext(Dispatchers.IO) {
        runCatching { AppLocalDB.db.recipeDao.insertRecipes(recipe) }
        runCatching { firebaseModel.addRecipeSync(recipe) }
    }

    suspend fun deleteRecipe(recipe: Recipe): Unit = withContext(Dispatchers.IO) {
        runCatching { AppLocalDB.db.recipeDao.deleteRecipe(recipe) }
        runCatching { firebaseModel.deleteRecipeSync(recipe) }
    }

    suspend fun getRecipeById(id: String): Recipe? = withContext(Dispatchers.IO) {
        val local: Recipe? = runCatching { AppLocalDB.db.recipeDao.getRecipeById(id) }.getOrNull()
        val remote: Recipe? = runCatching { firebaseModel.getRecipeByIdSync(id) }.getOrNull()
        if (remote != null) {
            runCatching { AppLocalDB.db.recipeDao.insertRecipes(remote) }
        }
        remote ?: local
    }
}
