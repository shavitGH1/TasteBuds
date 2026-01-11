package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
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
        val layout = LinearLayoutManager(context)
        binding?.recyclerView?.layoutManager = layout
        binding?.recyclerView?.setHasFixedSize(true)

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
        }

    }

    private fun navigateToPinkFragment(recipe: Recipe) {
        view?.let {
            val action = RecipesListFragmentDirections.actionRecipesListFragmentToBlueFragment(recipe.name)
            Navigation.findNavController(it).navigate(action)
        }
    }
}