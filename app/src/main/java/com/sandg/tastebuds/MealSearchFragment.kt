package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.sandg.tastebuds.databinding.FragmentMealSearchBinding

class MealSearchFragment : Fragment() {

    private var binding: FragmentMealSearchBinding? = null
    private val viewModel: MealSearchViewModel by viewModels()
    private lateinit var adapter: MealSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMealSearchBinding.inflate(inflater, container, false)
        setupUI()
        observeViewModel()
        return binding?.root
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupUI() {
        adapter = MealSearchAdapter { meal -> viewModel.loadMealDetail(meal) }
        binding?.mealsRecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        binding?.mealsRecyclerView?.adapter = adapter
        binding?.searchButton?.setOnClickListener { performSearch() }
        binding?.ingredientEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { performSearch(); true } else false
        }
    }

    private fun performSearch() {
        val ingredient = binding?.ingredientEditText?.text?.toString()?.trim() ?: ""
        if (ingredient.isEmpty()) { showStyledToast("Please enter an ingredient to search"); return }
        hideKeyboard()
        viewModel.search(ingredient)
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.searchState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MealSearchState.Loading -> setSearchLoading(true)
                is MealSearchState.Results -> {
                    setSearchLoading(false)
                    adapter.updateMeals(state.meals)
                    binding?.resultsCountText?.text = getString(R.string.meal_search_results, state.meals.size, state.query)
                    binding?.resultsCountText?.visibility = View.VISIBLE
                    binding?.emptyStateText?.visibility = View.GONE
                }
                is MealSearchState.Empty -> {
                    setSearchLoading(false)
                    adapter.updateMeals(emptyList())
                    binding?.emptyStateText?.text = getString(R.string.meal_search_empty, state.query)
                    binding?.emptyStateText?.visibility = View.VISIBLE
                    binding?.resultsCountText?.visibility = View.GONE
                }
                is MealSearchState.Error -> {
                    setSearchLoading(false)
                    binding?.emptyStateText?.text = getString(R.string.meal_search_network_error)
                    binding?.emptyStateText?.visibility = View.VISIBLE
                    showStyledToast(state.message)
                }
                else -> Unit
            }
        }

        viewModel.detailState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MealDetailState.Loading -> setDetailLoading(true)
                is MealDetailState.Ready -> {
                    setDetailLoading(false)
                    viewModel.resetDetailState()
                    try { findNavController().navigate(R.id.action_global_addRecipeFragment, state.bundle) }
                    catch (_: Exception) { showStyledToast("Navigation error") }
                }
                is MealDetailState.Error -> {
                    setDetailLoading(false)
                    viewModel.resetDetailState()
                    showStyledToast(state.message)
                }
                else -> Unit
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setSearchLoading(show: Boolean) {
        binding?.loadingProgress?.visibility = if (show) View.VISIBLE else View.GONE
        binding?.mealsRecyclerView?.visibility = if (show) View.GONE else View.VISIBLE
        binding?.searchButton?.isEnabled = !show
    }

    private fun setDetailLoading(show: Boolean) {
        binding?.loadingProgress?.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding?.ingredientEditText?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}

