package com.sandg.tastebuds.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Recipe(

    @PrimaryKey
    val id: String,

    val name: String,
    var isFavorite: Boolean,
    val imageUrlString: String?
) {

    companion object {

        fun fromJson(json: Map<String, Any?>): Recipe {
            val id = json["id"] as String
            val name = json["name"] as String
            val isFavorite = json["isFavorite"] as Boolean
            val imageUrlString = json["imageUrlString"] as String?

            return Recipe(
                id = id,
                name = name,
                isFavorite = isFavorite,
                imageUrlString = imageUrlString
            )
        }
    }

    val toJson: Map<String, Any?>
        get() = hashMapOf(
            "id" to id,
            "name" to name,
            "isFavorite" to isFavorite,
            "imageUrlString" to imageUrlString
        )
}