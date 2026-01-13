package com.sandg.tastebuds.models

import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.RecipeCompletion
import com.sandg.tastebuds.base.RecipesCompletion

class Model private constructor() {

    private val firebaseModel = FirebaseModel()
    private val firebaseAuth = FirebaseAuthModel()

    companion object {
        val shared = Model()
    }

    fun getAllRecipes(completion: RecipesCompletion) {


        firebaseAuth.signIn("shavitp21@gmail.com", "123456") {

        }

        firebaseModel.getAllRecipes(completion)
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        firebaseModel.addRecipe(recipe, completion)
    }

    fun deleteRecipe(recipe: Recipe) {
        firebaseModel.deleteRecipe(recipe)
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        firebaseModel.getRecipeById(id, completion)
    }
}

