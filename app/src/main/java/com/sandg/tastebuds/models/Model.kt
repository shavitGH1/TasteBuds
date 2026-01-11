package com.sandg.tastebuds.models

import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.RecipeCompletion
import com.sandg.tastebuds.base.RecipesCompletion

class Model private constructor() {

    private val firebaseModel = FirebaseModel()
    private val firebaseAuth = FirebaseAuthModel()

//    private val executor = Executors.newSingleThreadExecutor()
//    private val mainHandler = Handler.createAsync(Looper.getMainLooper())
//
//    private val database: AppLocalDbRepository = AppLocalDB.db

    companion object {
        val shared = Model()
    }

    fun getAllRecipes(completion: RecipesCompletion) {


        firebaseAuth.signIn("tal.ziii@colman.ac.il", "123456") {

        }

        firebaseModel.getAllRecipes(completion)
//
//        executor.execute {
//            val recipes = database.recipeDao.getAllRecipes()
//            mainHandler.post {
//                completion(recipes)
//            }
//        }
    }

    fun addRecipe(recipe: Recipe, completion: Completion) {
        firebaseModel.addRecipe(recipe, completion)

//        executor.execute {
//            database.recipeDao.insertRecipes(recipe)
//            mainHandler.post {
//                completion()
//            }
//        }
    }

    fun deleteRecipe(recipe: Recipe) {
        firebaseModel.deleteRecipe(recipe)
//        database.recipeDao.deleteRecipe(recipe)
    }

    fun getRecipeById(id: String, completion: RecipeCompletion) {
        firebaseModel.getRecipeById(id, completion)

//        executor.execute {
//            val recipe = database.recipeDao.getRecipeById(id)
//            mainHandler.post {
//                completion(recipe)
//            }
//        }
    }
}

