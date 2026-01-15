package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.sandg.tastebuds.databinding.FragmentRecipesListBinding
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe

class RecipesListFragment : Fragment() {

    private var binding: FragmentRecipesListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecipesListBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        return binding?.root
    }

    private fun setupRecyclerView() {
        // Use a vertical list so each item is a full-width recipe_row_layout card
        val layout = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding?.recyclerView?.layoutManager = layout
        binding?.recyclerView?.setHasFixedSize(true)

        // Add vertical spacing (12dp) between rows for a card-like gap
        val density = resources.displayMetrics.density
        val spacingPx = (12 * density).toInt()
        binding?.recyclerView?.addItemDecoration(SpacingItemDecoration(spacingPx))

        binding?.progressBar?.visibility = View.VISIBLE
        Model.shared.getAllRecipes { recipes ->

            binding?.progressBar?.visibility = View.GONE
            val adapter = RecipesAdapter(recipes)
            adapter.listener = object : OnItemClickListener {

                override fun onRecipeItemClick(recipe: Recipe) {
                    navigateToPinkFragment(recipe)
                }
            }
            binding?.recyclerView?.adapter = adapter

            // FAB: open Add Recipe
            binding?.addRecipeFab?.setOnClickListener {
                view?.findNavController()?.navigate(R.id.action_global_addRecipeFragment)
            }
        }

    }

    private fun navigateToPinkFragment(recipe: Recipe) {
        view?.findNavController()?.navigate(
            RecipesListFragmentDirections.actionRecipesListFragmentToBlueFragment(recipe.name)
        )
    }
}