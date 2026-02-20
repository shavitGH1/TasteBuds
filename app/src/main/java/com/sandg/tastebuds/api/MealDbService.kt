package com.sandg.tastebuds.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MealDbService {

    /**
     * Search meals by ingredient.
     * Example: filter.php?i=chicken
     */
    @GET("filter.php")
    fun filterByIngredient(@Query("i") ingredient: String): Call<MealSearchResponse>

    /**
     * Lookup full meal details by ID.
     * Example: lookup.php?i=52772
     */
    @GET("lookup.php")
    fun lookupMeal(@Query("i") mealId: String): Call<MealDetailResponse>
}

