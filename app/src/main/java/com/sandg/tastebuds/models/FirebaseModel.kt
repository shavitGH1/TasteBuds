package com.sandg.tastebuds.models

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.RecipeCompletion
import com.sandg.tastebuds.base.RecipesCompletion

class FirebaseModel {

    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val RECIPES = "recipes"
    }

    fun getAllRecipes(completion: RecipesCompletion) {
        db.collection(RECIPES).get()
            .addOnCompleteListener { result ->
                when (result.isSuccessful) {
                    true -> completion(result.result.map { Recipe.fromJson(it.data) })
                    false -> completion(emptyList())
                }
            }
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        db.collection(RECIPES)
            .document(recipe.id)
            .set(recipe.toJson)
            .addOnSuccessListener { documentReference ->
                completion()
            }
            .addOnFailureListener { e ->
                completion()
            }
    }

    fun deleteRecipe(recipe: Recipe) {
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        db.collection(COLLECTIONS.RECIPES).document(id).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.data != null) {
                    completion(Recipe.fromJson(doc.data!!))
                } else {
                    completion(Recipe(id = id, name = ""))
                }
            }
            .addOnFailureListener {
                completion(Recipe(id = id, name = ""))
            }
    }
}
