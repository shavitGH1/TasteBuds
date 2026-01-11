package com.sandg.tastebuds.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sandg.tastebuds.models.Recipe

@Dao
interface RecipeDao {

    @Query("SELECT * FROM Recipe")
    fun getAllRecipes(): List<Recipe>

    @Query("SELECT * FROM Recipe WHERE id = :id")
    fun getRecipeById(id: String): Recipe

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecipes(vararg recipes: Recipe)

    @Delete
    fun deleteRecipe(recipe: Recipe)
}