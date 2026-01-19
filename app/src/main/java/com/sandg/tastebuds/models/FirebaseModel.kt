package com.sandg.tastebuds.models

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.RecipeCompletion
import com.sandg.tastebuds.base.RecipesCompletion

class FirebaseModel {

    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val RECIPES = "recipes"
        const val USERS = "users"
    }

    // --- User helpers ---
    fun createOrUpdateUserDocument(uid: String, name: String, email: String, completion: (Boolean, Throwable?) -> Unit) {
        Log.d("FB_FIRESTORE", "createOrUpdateUserDocument: uid=$uid")
        val userDoc = mapOf("userId" to uid, "name" to name, "email" to email)
        db.collection(USERS).document(uid)
            .set(userDoc)
            .addOnSuccessListener {
                Log.d("FB_FIRESTORE", "createOrUpdateUserDocument: success uid=$uid")
                completion(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("FB_FIRESTORE", "createOrUpdateUserDocument: failure uid=$uid", e)
                completion(false, e)
            }
    }

    fun getUserById(uid: String, completion: (Map<String, Any?>?) -> Unit) {
        Log.d("FB_FIRESTORE", "getUserById: uid=$uid")
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.data != null) {
                    completion(doc.data)
                } else {
                    completion(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FB_FIRESTORE", "getUserById: failure uid=$uid", e)
                completion(null)
            }
    }

    fun findUserByName(name: String, completion: (Map<String, Any?>?) -> Unit) {
        Log.d("FB_FIRESTORE", "findUserByName: name=$name")
        db.collection(USERS).whereEqualTo("name", name).get()
            .addOnSuccessListener { snap ->
                if (snap != null && !snap.isEmpty) {
                    val doc = snap.documents.firstOrNull()
                    completion(doc?.data)
                } else {
                    completion(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FB_FIRESTORE", "findUserByName: failure name=$name", e)
                completion(null)
            }
    }

    // --- Recipe helpers ---
    fun getAllRecipes(completion: RecipesCompletion) {
        Log.d("FB_FIRESTORE", "getAllRecipes: querying collection $RECIPES")

        // Try to filter by current user's uid so each user sees only their recipes
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (!currentUid.isNullOrEmpty()) {
            Log.d("FB_FIRESTORE", "getAllRecipes: current user uid=$currentUid - fetching all and filtering client-side to accept both publisher_id and publisherId")
            // Fetch all recipes and filter client-side to support both field names used in documents
            db.collection(RECIPES).get()
                .addOnCompleteListener { result ->
                    when (result.isSuccessful) {
                        true -> {
                            val all = result.result.map { it.data }
                            val filtered = all.filter { doc ->
                                val pid1 = doc["publisher_id"] as? String
                                val pid2 = doc["publisherId"] as? String
                                pid1 == currentUid || pid2 == currentUid
                            }.map { Recipe.fromJson(it) }
                            Log.d("FB_FIRESTORE", "getAllRecipes: filtered count=${filtered.size}")
                            completion(filtered)
                        }
                        false -> {
                            Log.e("FB_FIRESTORE", "getAllRecipes: failed to fetch recipes", result.exception)
                            completion(emptyList())
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FB_FIRESTORE", "getAllRecipes: failure", e)
                    completion(emptyList())
                }
        } else {
            Log.d("FB_FIRESTORE", "getAllRecipes: no current user, returning all recipes")
            db.collection(RECIPES).get()
                .addOnCompleteListener { result ->
                    when (result.isSuccessful) {
                        true -> {
                            val list = result.result.map { Recipe.fromJson(it.data) }
                            Log.d("FB_FIRESTORE", "getAllRecipes: success count=${list.size}")
                            completion(list)
                        }
                        false -> {
                            Log.e("FB_FIRESTORE", "getAllRecipes: failed", result.exception)
                            completion(emptyList())
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FB_FIRESTORE", "getAllRecipes: failure", e)
                    completion(emptyList())
                }
        }
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        Log.d("FB_FIRESTORE", "addRecipe: writing recipe id=${recipe.id} to $RECIPES")
        db.collection(RECIPES)
            .document(recipe.id)
            .set(recipe.toJson)
            .addOnSuccessListener {
                Log.d("FB_FIRESTORE", "addRecipe: success id=${recipe.id}")
                completion()
            }
            .addOnFailureListener { e ->
                Log.e("FB_FIRESTORE", "addRecipe: failure id=${recipe.id}", e)
                completion()
            }
    }

    fun deleteRecipe(recipe: Recipe) {
        Log.d("FB_FIRESTORE", "deleteRecipe: deleting id=${recipe.id}")
        db.collection(COLLECTIONS.RECIPES).document(recipe.id).delete()
            .addOnSuccessListener {
                Log.d("FB_FIRESTORE", "deleteRecipe: success id=${recipe.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FB_FIRESTORE", "deleteRecipe: failure id=${recipe.id}", e)
            }
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        Log.d("FB_FIRESTORE", "getRecipeById: fetching id=$id")
        db.collection(COLLECTIONS.RECIPES).document(id).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.data != null) {
                    Log.d("FB_FIRESTORE", "getRecipeById: success id=$id")
                    completion(Recipe.fromJson(doc.data!!))
                } else {
                    Log.w("FB_FIRESTORE", "getRecipeById: not found id=$id")
                    completion(Recipe(id = id, name = ""))
                }
            }
            .addOnFailureListener { e ->
                Log.e("FB_FIRESTORE", "getRecipeById: failure id=$id", e)
                completion(Recipe(id = id, name = ""))
            }
    }
}
