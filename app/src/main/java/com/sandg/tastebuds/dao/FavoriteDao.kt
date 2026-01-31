package com.sandg.tastebuds.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sandg.tastebuds.models.Favorite

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM Favorite WHERE userId = :userId")
    fun getFavoritesForUser(userId: String): List<Favorite>

    @Query("SELECT * FROM Favorite WHERE recipeId = :recipeId AND userId = :userId LIMIT 1")
    fun getFavorite(recipeId: String, userId: String): Favorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(favorite: Favorite)

    @Query("DELETE FROM Favorite WHERE recipeId = :recipeId AND userId = :userId")
    fun deleteFavorite(recipeId: String, userId: String)
}

