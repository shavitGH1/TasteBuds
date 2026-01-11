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

        binding?.cancelButton?.setOnClickListener {
            dismiss()
        }

        binding?.saveRecipeButton?.setOnClickListener {

            binding?.loadingIndicator?.visibility = View.VISIBLE

            val recipeName: String = binding?.nameEditText?.text.toString()
            val recipeId: String = binding?.idEditText?.text.toString()

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
    }


    private fun dismiss() {
        view?.findNavController()?.popBackStack()
    }
}