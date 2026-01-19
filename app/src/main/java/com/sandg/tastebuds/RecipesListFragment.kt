package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.fragment.app.activityViewModels
import com.sandg.tastebuds.databinding.FragmentRecipesListBinding
import com.sandg.tastebuds.models.Recipe

class RecipesListFragment : Fragment() {

    private var binding: FragmentRecipesListBinding? = null
    private val sharedVm: SharedRecipesViewModel by activityViewModels()
    private lateinit var adapter: RecipesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecipesListBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        return binding?.root
    }

    private fun setupRecyclerView() {
        val layout = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding?.recyclerView?.layoutManager = layout
        binding?.recyclerView?.setHasFixedSize(true)

        adapter = RecipesAdapter()
        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                navigateToRecipeDetail(recipe)
            }

            override fun onToggleFavorite(recipe: Recipe) {
                sharedVm.toggleFavorite(recipe)
            }
        }

        binding?.recyclerView?.adapter = adapter

        val density = resources.displayMetrics.density
        val spacingPx = (12 * density).toInt()
        binding?.recyclerView?.addItemDecoration(SpacingItemDecoration(spacingPx))

        binding?.progressBar?.visibility = View.VISIBLE

        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            binding?.progressBar?.visibility = View.GONE
            adapter.submitList(list.toList())
        }

        binding?.addRecipeFab?.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_global_addRecipeFragment)
        }
    }


    private fun navigateToRecipeDetail(recipe: Recipe) {
        val args = bundleOf("recipeId" to recipe.id)
        view?.findNavController()?.navigate(R.id.action_recipesListFragment_to_recipeDetailFragment, args)
    }
}