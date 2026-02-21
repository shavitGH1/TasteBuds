package com.sandg.tastebuds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sandg.tastebuds.api.MealDbClient
import com.sandg.tastebuds.api.MealDetailResponse
import com.sandg.tastebuds.api.MealSearchResponse
import com.sandg.tastebuds.api.MealSummary
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

sealed class MealSearchState {
    object Idle : MealSearchState()
    object Loading : MealSearchState()
    data class Results(val meals: List<MealSummary>, val query: String) : MealSearchState()
    data class Empty(val query: String) : MealSearchState()
    data class Error(val message: String) : MealSearchState()
}

sealed class MealDetailState {
    object Idle : MealDetailState()
    object Loading : MealDetailState()
    data class Ready(val bundle: android.os.Bundle) : MealDetailState()
    data class Error(val message: String) : MealDetailState()
}

class MealSearchViewModel : ViewModel() {

    private val _searchState = MutableLiveData<MealSearchState>(MealSearchState.Idle)
    val searchState: LiveData<MealSearchState> = _searchState

    private val _detailState = MutableLiveData<MealDetailState>(MealDetailState.Idle)
    val detailState: LiveData<MealDetailState> = _detailState

    private var searchCall: Call<MealSearchResponse>? = null
    private var detailCall: Call<MealDetailResponse>? = null

    fun search(ingredient: String) {
        if (ingredient.isBlank()) return
        searchCall?.cancel()
        _searchState.value = MealSearchState.Loading

        searchCall = MealDbClient.service.filterByIngredient(ingredient)
        searchCall!!.enqueue(object : Callback<MealSearchResponse> {
            override fun onResponse(call: Call<MealSearchResponse>, response: Response<MealSearchResponse>) {
                val meals = response.body()?.meals
                _searchState.postValue(
                    if (meals.isNullOrEmpty()) MealSearchState.Empty(ingredient)
                    else MealSearchState.Results(meals, ingredient)
                )
            }
            override fun onFailure(call: Call<MealSearchResponse>, t: Throwable) {
                if (!call.isCanceled) {
                    _searchState.postValue(MealSearchState.Error(t.localizedMessage ?: "Network error"))
                }
            }
        })
    }

    fun loadMealDetail(meal: MealSummary) {
        detailCall?.cancel()
        _detailState.value = MealDetailState.Loading

        detailCall = MealDbClient.service.lookupMeal(meal.id)
        detailCall!!.enqueue(object : Callback<MealDetailResponse> {
            override fun onResponse(call: Call<MealDetailResponse>, response: Response<MealDetailResponse>) {
                val detail = response.body()?.meals?.firstOrNull()
                if (detail == null) {
                    _detailState.postValue(MealDetailState.Error("Could not load recipe details"))
                    return
                }
                val bundle = android.os.Bundle().apply {
                    putString("recipeName", detail.name ?: meal.name)
                    val desc = listOfNotNull(
                        detail.category?.let { "Category: $it" },
                        detail.area?.let { "Cuisine: $it" },
                        detail.tags?.let { "Tags: $it" }
                    ).joinToString(" • ")
                    if (desc.isNotEmpty()) putString("description", desc)
                    putInt("time", 30)
                    detail.thumbUrl?.let { putString("imageUrl", it) }
                    val ingredients = detail.toIngredientList()
                    if (ingredients.isNotEmpty()) putString("ingredientsJson", com.google.gson.Gson().toJson(ingredients))
                    val steps = detail.toStepsList()
                    if (steps.isNotEmpty()) putStringArrayList("steps", ArrayList(steps))
                }
                _detailState.postValue(MealDetailState.Ready(bundle))
            }
            override fun onFailure(call: Call<MealDetailResponse>, t: Throwable) {
                if (!call.isCanceled) {
                    _detailState.postValue(MealDetailState.Error(t.localizedMessage ?: "Network error"))
                }
            }
        })
    }

    fun resetDetailState() { _detailState.value = MealDetailState.Idle }

    override fun onCleared() {
        super.onCleared()
        searchCall?.cancel()
        detailCall?.cancel()
    }
}

