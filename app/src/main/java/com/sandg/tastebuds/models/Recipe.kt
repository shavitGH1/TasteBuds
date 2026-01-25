package com.sandg.tastebuds.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Recipe(

    @PrimaryKey
    val id: String,

    val name: String,
    var isFavorite: Boolean = false,
    val imageUrlString: String? = null,

    val publisher: String? = null,
    val publisherId: String? = null,
    val ingredients: List<Ingredient> = listOf(),
    val steps: List<String> = listOf(),
    val time: Int? = null,
    val difficulty: String? = null,
    val dietRestrictions: List<String> = listOf(),
    val description: String? = null
) {

    companion object {

        fun fromJson(json: Map<String, Any?>): Recipe {
            val id = json["id"] as? String ?: ""
            val name = json["name"] as? String ?: ""
            val isFavorite = json["isFavorite"] as? Boolean ?: false
            val imageUrlString = json["imageUrlString"] as? String

            val publisher = json["publisher"] as? String
            // Accept both server-side field names: 'publisher_id' (snake) and 'publisherId' (camel)
            val publisherId = (json["publisher_id"] as? String)
                ?: (json["publisherId"] as? String)

            val ingredients = (json["ingredients"] as? List<*>)?.mapNotNull { item ->
                (item as? Map<*, *>)?.let { Ingredient.fromJson(it) }
            } ?: listOf()

            val steps = (json["steps"] as? List<*>)?.mapNotNull { it as? String } ?: listOf()

            val time = when (val t = json["time"]) {
                is Number -> t.toInt()
                is String -> t.toIntOrNull()
                else -> null
            }

            val difficulty = json["difficulty"] as? String

            val dietRestrictions = (json["dietRestrictions"] as? List<*>)?.mapNotNull { it as? String } ?: listOf()

            val description = json["description"] as? String

            return Recipe(
                id = id,
                name = name,
                isFavorite = isFavorite,
                imageUrlString = imageUrlString,
                publisher = publisher,
                publisherId = publisherId,
                ingredients = ingredients,
                steps = steps,
                time = time,
                difficulty = difficulty,
                dietRestrictions = dietRestrictions,
                description = description
            )
        }
    }

    val toJson: Map<String, Any?>
        get() = hashMapOf(
            "id" to id,
            "name" to name,
            "imageUrlString" to imageUrlString,
            "publisher" to publisher,
            "publisher_id" to publisherId,
            "ingredients" to ingredients.map { it.toJson() },
            "steps" to steps,
            "time" to time,
            "difficulty" to difficulty,
            "dietRestrictions" to dietRestrictions,
            "description" to description,
            "isFavorite" to isFavorite
        )
}

data class Ingredient(
    val name: String,
    val amount: Double? = null,
    val unit: String? = null
) {
    fun toJson(): Map<String, Any?> = hashMapOf(
        "name" to name,
        "amount" to amount,
        "unit" to unit
    )

    companion object {
        fun fromJson(map: Map<*, *>): Ingredient {
            val name = map["name"] as? String ?: ""
            val amount = when (val a = map["amount"]) {
                is Number -> a.toDouble()
                is String -> a.toDoubleOrNull()
                else -> null
            }
            val unit = map["unit"] as? String
            return Ingredient(name = name, amount = amount, unit = unit)
        }
    }
}
