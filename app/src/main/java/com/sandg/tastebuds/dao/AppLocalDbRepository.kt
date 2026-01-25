package com.sandg.tastebuds.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sandg.tastebuds.models.Favorite
import com.sandg.tastebuds.models.Recipe

@Database(entities = [Recipe::class, Favorite::class], version = 5)
@TypeConverters(Converters::class)
abstract class AppLocalDbRepository: RoomDatabase() {
    abstract val recipeDao: RecipeDao
    abstract val favoriteDao: FavoriteDao
}