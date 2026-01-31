package com.sandg.tastebuds.models

import androidx.room.Entity

@Entity(primaryKeys = ["recipeId", "userId"]) // composite key
data class Favorite(
    val recipeId: String,
    val userId: String,
    val isFavorite: Boolean = true
)

