package com.sandg.tastebuds.api

import com.google.gson.annotations.SerializedName
import com.sandg.tastebuds.models.Ingredient

/**
 * Response from filter.php?i=<ingredient>
 */
data class MealSearchResponse(
    @SerializedName("meals") val meals: List<MealSummary>?
)

data class MealSummary(
    @SerializedName("strMeal") val name: String,
    @SerializedName("strMealThumb") val thumbUrl: String,
    @SerializedName("idMeal") val id: String
)

/**
 * Response from lookup.php?i=<id>
 */
data class MealDetailResponse(
    @SerializedName("meals") val meals: List<MealDetail>?
)

data class MealDetail(
    @SerializedName("idMeal") val id: String?,
    @SerializedName("strMeal") val name: String?,
    @SerializedName("strCategory") val category: String?,
    @SerializedName("strArea") val area: String?,
    @SerializedName("strInstructions") val instructions: String?,
    @SerializedName("strMealThumb") val thumbUrl: String?,
    @SerializedName("strTags") val tags: String?,
    @SerializedName("strYoutube") val youtubeUrl: String?,
    @SerializedName("strIngredient1") val ingredient1: String?,
    @SerializedName("strIngredient2") val ingredient2: String?,
    @SerializedName("strIngredient3") val ingredient3: String?,
    @SerializedName("strIngredient4") val ingredient4: String?,
    @SerializedName("strIngredient5") val ingredient5: String?,
    @SerializedName("strIngredient6") val ingredient6: String?,
    @SerializedName("strIngredient7") val ingredient7: String?,
    @SerializedName("strIngredient8") val ingredient8: String?,
    @SerializedName("strIngredient9") val ingredient9: String?,
    @SerializedName("strIngredient10") val ingredient10: String?,
    @SerializedName("strIngredient11") val ingredient11: String?,
    @SerializedName("strIngredient12") val ingredient12: String?,
    @SerializedName("strIngredient13") val ingredient13: String?,
    @SerializedName("strIngredient14") val ingredient14: String?,
    @SerializedName("strIngredient15") val ingredient15: String?,
    @SerializedName("strIngredient16") val ingredient16: String?,
    @SerializedName("strIngredient17") val ingredient17: String?,
    @SerializedName("strIngredient18") val ingredient18: String?,
    @SerializedName("strIngredient19") val ingredient19: String?,
    @SerializedName("strIngredient20") val ingredient20: String?,
    @SerializedName("strMeasure1") val measure1: String?,
    @SerializedName("strMeasure2") val measure2: String?,
    @SerializedName("strMeasure3") val measure3: String?,
    @SerializedName("strMeasure4") val measure4: String?,
    @SerializedName("strMeasure5") val measure5: String?,
    @SerializedName("strMeasure6") val measure6: String?,
    @SerializedName("strMeasure7") val measure7: String?,
    @SerializedName("strMeasure8") val measure8: String?,
    @SerializedName("strMeasure9") val measure9: String?,
    @SerializedName("strMeasure10") val measure10: String?,
    @SerializedName("strMeasure11") val measure11: String?,
    @SerializedName("strMeasure12") val measure12: String?,
    @SerializedName("strMeasure13") val measure13: String?,
    @SerializedName("strMeasure14") val measure14: String?,
    @SerializedName("strMeasure15") val measure15: String?,
    @SerializedName("strMeasure16") val measure16: String?,
    @SerializedName("strMeasure17") val measure17: String?,
    @SerializedName("strMeasure18") val measure18: String?,
    @SerializedName("strMeasure19") val measure19: String?,
    @SerializedName("strMeasure20") val measure20: String?
) {

    /**
     * Collects the 20 ingredient/measure pairs into a list, filtering blanks.
     */
    fun toIngredientList(): List<Ingredient> {
        val ingredients = listOf(
            ingredient1 to measure1, ingredient2 to measure2, ingredient3 to measure3,
            ingredient4 to measure4, ingredient5 to measure5, ingredient6 to measure6,
            ingredient7 to measure7, ingredient8 to measure8, ingredient9 to measure9,
            ingredient10 to measure10, ingredient11 to measure11, ingredient12 to measure12,
            ingredient13 to measure13, ingredient14 to measure14, ingredient15 to measure15,
            ingredient16 to measure16, ingredient17 to measure17, ingredient18 to measure18,
            ingredient19 to measure19, ingredient20 to measure20
        )

        return ingredients.mapNotNull { (name, measure) ->
            if (name.isNullOrBlank()) return@mapNotNull null
            val measureTrimmed = measure?.trim() ?: ""
            // Try to parse measure into amount + unit, e.g. "3/4 cup" → amount=null, unit="3/4 cup"
            Ingredient(name = name.trim(), amount = null, unit = measureTrimmed.ifEmpty { null })
        }
    }

    /**
     * Splits the instruction text into individual steps.
     */
    fun toStepsList(): List<String> {
        if (instructions.isNullOrBlank()) return emptyList()
        // Split by \r\n or \n, filter blanks
        return instructions
            .split("\r\n", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

