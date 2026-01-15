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
        firebaseAuth.signIn("shavitp21@gmail.com", "123456") {
        }

        Thread {
            try {
                val local = AppLocalDB.db.recipeDao.getAllRecipes()
                if (local.isNotEmpty()) {
                    completion(local)
                }
            } catch (e: Exception) {
            }

            firebaseModel.getAllRecipes { remoteList ->
                Thread {
                    try {
                        if (remoteList.isNotEmpty()) {
                            AppLocalDB.db.recipeDao.insertRecipes(*remoteList.toTypedArray())
                        }
                    } catch (e: Exception) {
                    }
                }.start()

                completion(remoteList)
            }
        }.start()
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        Thread {
            try {
                AppLocalDB.db.recipeDao.insertRecipes(recipe)
            } catch (e: Exception) {
            }
        }.start()

        firebaseModel.addRecipe(recipe, completion)
    }

    fun deleteRecipe(recipe: Recipe) {
        firebaseModel.deleteRecipe(recipe)
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        Thread {
            try {
                val local = AppLocalDB.db.recipeDao.getRecipeById(id)
                if (local != null) {
                    completion(local)
                }
            } catch (e: Exception) {
            }

            firebaseModel.getRecipeById(id) { remote ->
                Thread {
                    try {
                        AppLocalDB.db.recipeDao.insertRecipes(remote)
                    } catch (e: Exception) {
                    }
                }.start()

                completion(remote)
            }
        }.start()
    }
}
