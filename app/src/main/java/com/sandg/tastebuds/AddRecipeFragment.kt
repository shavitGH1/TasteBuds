package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.sandg.tastebuds.databinding.FragmentAddRecipeBinding
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe

class AddRecipeFragment : Fragment() {

    private var binding: FragmentAddRecipeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddRecipeBinding.inflate(layoutInflater, container, false)
        setupView()
        setHasOptionsMenu(true)
        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    private fun setupView() {

        binding?.loadingIndicator?.visibility = View.GONE

        binding?.saveRecipeButton?.setOnClickListener {

            binding?.loadingIndicator?.visibility = View.VISIBLE

            val recipeName: String = binding?.nameEditText?.text.toString()
            val preparationTime: String = binding?.preparationTimeEditText?.text.toString()
            val ingredients: String = binding?.ingredientsEditText?.text.toString()
            val preparationMethod: String = binding?.preparationMethodEditText?.text.toString()

            // For now, using recipe name as ID (you can change this to generate a unique ID)
            val recipeId: String = recipeName.replace(" ", "_").lowercase()

            val recipe = Recipe(
                name = recipeName,
                id = recipeId,
                isFavorite = false,
                imageUrlString = null
            )

            Model.shared.addRecipe(recipe) {
                dismiss()
            }
        }

        binding?.selectImageButton?.setOnClickListener {
            // TODO: Implement image selection functionality
        }
    }


    private fun dismiss() {
        view?.findNavController()?.popBackStack()
    }
}