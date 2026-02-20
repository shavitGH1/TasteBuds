package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.sandg.tastebuds.api.MealDbClient
import com.sandg.tastebuds.api.MealDetailResponse
import com.sandg.tastebuds.api.MealSearchResponse
import com.sandg.tastebuds.api.MealSummary
import com.sandg.tastebuds.databinding.FragmentMealSearchBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MealSearchFragment : Fragment() {

    private var binding: FragmentMealSearchBinding? = null
    private lateinit var adapter: MealSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMealSearchBinding.inflate(inflater, container, false)
        setupUI()
        return binding?.root
    }

    private fun setupUI() {
        adapter = MealSearchAdapter { meal ->
            onMealSelected(meal)
        }

        binding?.mealsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.mealsRecyclerView?.adapter = adapter

        binding?.searchButton?.setOnClickListener {
            performSearch()
        }

        // Allow searching with keyboard action button
        binding?.ingredientEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val ingredient = binding?.ingredientEditText?.text?.toString()?.trim() ?: ""
        if (ingredient.isEmpty()) {
            showStyledToast("Please enter an ingredient to search")
            return
        }

        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding?.ingredientEditText?.windowToken, 0)

        showLoading(true)
        binding?.emptyStateText?.visibility = View.GONE
        binding?.resultsCountText?.visibility = View.GONE

        MealDbClient.service.filterByIngredient(ingredient).enqueue(object : Callback<MealSearchResponse> {
            override fun onResponse(call: Call<MealSearchResponse>, response: Response<MealSearchResponse>) {
                activity?.runOnUiThread {
                    showLoading(false)
                    val meals = response.body()?.meals
                    if (meals.isNullOrEmpty()) {
                        adapter.updateMeals(emptyList())
                        binding?.emptyStateText?.text = "😕 No recipes found for \"$ingredient\"\nTry another ingredient!"
                        binding?.emptyStateText?.visibility = View.VISIBLE
                        binding?.resultsCountText?.visibility = View.GONE
                    } else {
                        adapter.updateMeals(meals)
                        binding?.resultsCountText?.text = "${meals.size} recipes found for \"$ingredient\""
                        binding?.resultsCountText?.visibility = View.VISIBLE
                        binding?.emptyStateText?.visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<MealSearchResponse>, t: Throwable) {
                activity?.runOnUiThread {
                    showLoading(false)
                    binding?.emptyStateText?.text = "⚠️ Network error. Please check your connection and try again."
                    binding?.emptyStateText?.visibility = View.VISIBLE
                    showStyledToast("Search failed: ${t.localizedMessage}")
                }
            }
        })
    }

    private fun onMealSelected(meal: MealSummary) {
        // Show a loading state while fetching full details
        showLoading(true)

        MealDbClient.service.lookupMeal(meal.id).enqueue(object : Callback<MealDetailResponse> {
            override fun onResponse(call: Call<MealDetailResponse>, response: Response<MealDetailResponse>) {
                activity?.runOnUiThread {
                    showLoading(false)
                    val detail = response.body()?.meals?.firstOrNull()
                    if (detail == null) {
                        showStyledToast("Could not load recipe details")
                        return@runOnUiThread
                    }

                    // Build a bundle with the recipe data to pre-fill AddRecipeFragment
                    val bundle = Bundle().apply {
                        putString("recipeName", detail.name ?: meal.name)

                        // Description from category + area
                        val desc = listOfNotNull(
                            detail.category?.let { "Category: $it" },
                            detail.area?.let { "Cuisine: $it" },
                            detail.tags?.let { "Tags: $it" }
                        ).joinToString(" • ")
                        if (desc.isNotEmpty()) putString("description", desc)

                        // Default time (30 min since API doesn't provide it)
                        putInt("time", 30)

                        // Image URL
                        detail.thumbUrl?.let { putString("imageUrl", it) }

                        // Ingredients as JSON
                        val ingredients = detail.toIngredientList()
                        if (ingredients.isNotEmpty()) {
                            putString("ingredientsJson", Gson().toJson(ingredients))
                        }

                        // Steps
                        val steps = detail.toStepsList()
                        if (steps.isNotEmpty()) {
                            putStringArrayList("steps", ArrayList(steps))
                        }
                    }

                    // Navigate to AddRecipeFragment with pre-filled data
                    try {
                        findNavController().navigate(R.id.action_global_addRecipeFragment, bundle)
                    } catch (e: Exception) {
                        showStyledToast("Navigation error: ${e.localizedMessage}")
                    }
                }
            }

            override fun onFailure(call: Call<MealDetailResponse>, t: Throwable) {
                activity?.runOnUiThread {
                    showLoading(false)
                    showStyledToast("Failed to load recipe: ${t.localizedMessage}")
                }
            }
        })
    }

    private fun showLoading(show: Boolean) {
        binding?.loadingProgress?.visibility = if (show) View.VISIBLE else View.GONE
        binding?.mealsRecyclerView?.visibility = if (show) View.GONE else View.VISIBLE
        binding?.searchButton?.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

