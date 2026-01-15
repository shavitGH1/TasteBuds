package com.sandg.tastebuds.models

import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.RecipeCompletion
import com.sandg.tastebuds.base.RecipesCompletion
import com.sandg.tastebuds.dao.AppLocalDB

class Model private constructor() {

    private val firebaseModel = FirebaseModel()
    private val firebaseAuth = FirebaseAuthModel()

    companion object {
        val shared = Model()
    }

    fun getAllRecipes(completion: RecipesCompletion) {

        // Try to sign in (kept from previous behavior)
        firebaseAuth.signIn("shavitp21@gmail.com", "123456") {

        }

        // First, return any locally cached recipes quickly (background thread)
        Thread {
            try {
                val local = AppLocalDB.db.recipeDao.getAllRecipes()
                if (local.isNotEmpty()) {
                    completion(local)
                }
            } catch (e: Exception) {
                // ignore local read errors
            }

            // Then fetch from remote and update local DB; final completion will return remote data
            firebaseModel.getAllRecipes { remoteList ->
                Thread {
                    try {
                        if (remoteList.isNotEmpty()) {
                            // insert/replace into local DB
                            AppLocalDB.db.recipeDao.insertRecipes(*remoteList.toTypedArray())
                        }
                    } catch (e: Exception) {
                        // ignore local write errors
                    }
                }.start()

                // deliver remote data to caller
                completion(remoteList)
            }
        }.start()
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        // Persist locally on a background thread (Room requires background thread for DB writes)
        Thread {
            try {
                AppLocalDB.db.recipeDao.insertRecipes(recipe)
            } catch (e: Exception) {
                // ignore local persistence errors for now
            }
        }.start()

        // Also persist remotely via Firebase
        firebaseModel.addRecipe(recipe, completion)
    }

    fun deleteRecipe(recipe: Recipe) {
        firebaseModel.deleteRecipe(recipe)
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        firebaseModel.getRecipeById(id, completion)
    }
}
