package com.sandg.tastebuds.models

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.MyApplication
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
        // 1) Load local recipes immediately so UI shows saved items on startup
        Thread {
            try {
                val local = AppLocalDB.db.recipeDao.getAllRecipes()
                if (local.isNotEmpty()) {
                    Handler(Looper.getMainLooper()).post { completion(local) }
                }
            } catch (e: Exception) {
            }
        }.start()

        // 2) Determine auth strategy: prefer existing Firebase user, then stored credentials
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // already signed in, fetch remote recipes
            firebaseModel.getAllRecipes { remoteList ->
                Thread {
                    try {
                        if (remoteList.isNotEmpty()) {
                            AppLocalDB.db.recipeDao.insertRecipes(*remoteList.toTypedArray())
                        }
                    } catch (e: Exception) {
                    }
                }.start()

                Handler(Looper.getMainLooper()).post { completion(remoteList) }
            }
            return
        }

        // No active Firebase user — try stored credentials (option 3)
        val prefs = MyApplication.appContext?.getSharedPreferences("auth", Context.MODE_PRIVATE)
        val storedEmail = prefs?.getString("email", null)
        val storedPassword = prefs?.getString("password", null)

        if (!storedEmail.isNullOrEmpty() && !storedPassword.isNullOrEmpty()) {
            // Sign in with stored credentials, then fetch remote recipes
            firebaseAuth.signIn(storedEmail, storedPassword) {
                firebaseModel.getAllRecipes { remoteList ->
                    Thread {
                        try {
                            if (remoteList.isNotEmpty()) {
                                AppLocalDB.db.recipeDao.insertRecipes(*remoteList.toTypedArray())
                            }
                        } catch (e: Exception) {
                        }
                    }.start()

                    Handler(Looper.getMainLooper()).post { completion(remoteList) }
                }
            }
        } else {
            // No stored credentials and not signed in — skip remote fetch (local-only)
            // If you prefer automatic anonymous sign-in, we can implement that here.
        }
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        // Insert locally on a background thread
        Thread {
            try {
                AppLocalDB.db.recipeDao.insertRecipes(recipe)
            } catch (e: Exception) {
            }
        }.start()

        // Call completion on main thread so UI code in the caller can run safely
        Handler(Looper.getMainLooper()).post { completion() }

        // Fire off the remote write asynchronously; don't rely on this to dismiss UI
        firebaseModel.addRecipe(recipe) { /* no-op: network result handled via logs */ }
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

    // Expose a direct remote fetch of all recipes (no local-first logic)
    fun getAllRemoteRecipes(completion: RecipesCompletion) {
        firebaseModel.getAllRemoteRecipes { list ->
            completion(list)
        }
    }
}
