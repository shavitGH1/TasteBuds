package com.sandg.tastebuds.models

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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
        val userDoc = mapOf("userId" to uid, "name" to name, "email" to email)
        db.collection(USERS).document(uid)
            .set(userDoc)
            .addOnSuccessListener {
                completion(true, null)
            }
            .addOnFailureListener { _ ->
                completion(false, null)
            }
    }

    fun getUserById(uid: String, completion: (Map<String, Any?>?) -> Unit) {
        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.data != null) {
                    completion(doc.data)
                } else {
                    completion(null)
                }
            }
            .addOnFailureListener { _ ->
                completion(null)
            }
    }

    fun findUserByName(name: String, completion: (Map<String, Any?>?) -> Unit) {
        db.collection(USERS).whereEqualTo("name", name).get()
            .addOnSuccessListener { snap ->
                if (snap != null && !snap.isEmpty) {
                    val doc = snap.documents.firstOrNull()
                    completion(doc?.data)
                } else {
                    completion(null)
                }
            }
            .addOnFailureListener { _ ->
                completion(null)
            }
    }

    // --- Recipe helpers ---
    fun getAllRecipes(completion: RecipesCompletion) {

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        if (!currentUid.isNullOrEmpty()) {
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
                            completion(filtered)
                        }
                        false -> {
                            completion(emptyList())
                        }
                    }
                }
                .addOnFailureListener { _ ->
                    completion(emptyList())
                }
        } else {
            db.collection(RECIPES).get()
                .addOnCompleteListener { result ->
                    when (result.isSuccessful) {
                        true -> {
                            val list = result.result.map { Recipe.fromJson(it.data) }
                            completion(list)
                        }
                        false -> {
                            completion(emptyList())
                        }
                    }
                }
                .addOnFailureListener { _ ->
                    completion(emptyList())
                }
        }
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        db.collection(RECIPES)
            .document(recipe.id)
            .set(recipe.toJson)
            .addOnSuccessListener {
                completion()
            }
            .addOnFailureListener { _ ->
                completion()
            }
    }

    fun deleteRecipe(recipe: Recipe) {
        db.collection(RECIPES).document(recipe.id).delete()
            .addOnSuccessListener {
            }
            .addOnFailureListener { _ ->
            }
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        db.collection(RECIPES).document(id).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.data != null) {
                    completion(Recipe.fromJson(doc.data!!))
                } else {
                    completion(Recipe(id = id, name = ""))
                }
            }
            .addOnFailureListener { _ ->
                completion(Recipe(id = id, name = ""))
            }
    }

    // Return all recipes from Firestore regardless of current user
    fun getAllRemoteRecipes(completion: RecipesCompletion) {
        db.collection(RECIPES).get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { it.data?.let { Recipe.fromJson(it) } }
                completion(list)
            }
            .addOnFailureListener { _ ->
                completion(emptyList())
            }
    }
}
