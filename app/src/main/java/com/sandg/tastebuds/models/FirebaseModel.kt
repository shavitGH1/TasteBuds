package com.sandg.tastebuds.models

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirebaseModel {

    private val db = Firebase.firestore

    private companion object {
        const val RECIPES = "recipes"
        const val USERS = "users"
    }

    // ── Recipe operations ─────────────────────────────────────────────────────

    suspend fun getAllRecipesSync(): List<Recipe> {
        val snap = db.collection(RECIPES).get().await()
        return snap.documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap() ?: return@mapNotNull null
            if (!data.containsKey("id")) data["id"] = doc.id
            Recipe.fromJson(data)
        }
    }

    suspend fun addRecipeSync(recipe: Recipe) {
        db.collection(RECIPES).document(recipe.id).set(recipe.toJson).await()
    }

    suspend fun deleteRecipeSync(recipe: Recipe) {
        db.collection(RECIPES).document(recipe.id).delete().await()
    }

    suspend fun getRecipeByIdSync(id: String): Recipe? {
        val doc = db.collection(RECIPES).document(id).get().await()
        val data = doc.data?.toMutableMap() ?: return null
        if (!data.containsKey("id")) data["id"] = doc.id
        return Recipe.fromJson(data)
    }

    // ── User operations ───────────────────────────────────────────────────────

    suspend fun createOrUpdateUserDocumentSync(uid: String, name: String, email: String) {
        val userDoc = mapOf("userId" to uid, "name" to name, "email" to email)
        db.collection(USERS).document(uid).set(userDoc).await()
    }

    suspend fun isUsernameTakenSync(name: String): Boolean {
        val snap = db.collection(USERS).whereEqualTo("name", name).get().await()
        return !snap.isEmpty
    }

    suspend fun getUserByIdSync(uid: String): Map<String, Any?>? {
        val doc = db.collection(USERS).document(uid).get().await()
        return doc.data
    }
}
