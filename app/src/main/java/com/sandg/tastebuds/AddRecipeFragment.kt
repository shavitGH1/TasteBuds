package com.sandg.tastebuds

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.findNavController
import com.sandg.tastebuds.databinding.FragmentAddRecipeBinding
import com.sandg.tastebuds.models.Ingredient
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe

class AddRecipeFragment : Fragment() {

    private var binding: FragmentAddRecipeBinding? = null

    private val ingredientsList = mutableListOf<Ingredient>()
    private val stepsList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddRecipeBinding.inflate(layoutInflater, container, false)
        setupView()
        return binding?.root
    }

    private fun setupView() {

        binding?.loadingIndicator?.visibility = View.GONE

        binding?.preparationTimeText?.text = getString(R.string.default_time)
        binding?.difficultyText?.text = getString(R.string.default_difficulty)
        binding?.dietRestrictionsText?.text = getString(R.string.default_diet)

        binding?.addStepButton?.setOnClickListener {
            addStepEditText("")
        }

        binding?.addIngredientButton?.setOnClickListener {
            showAddIngredientDialog()
        }

        binding?.saveRecipeButton?.setOnClickListener {

            // Prevent double-clicks while saving
            binding?.saveRecipeButton?.isEnabled = false
            binding?.loadingIndicator?.visibility = View.VISIBLE

            val recipeName: String = binding?.nameEditText?.text.toString().trim()
            if (recipeName.isEmpty()) {
                binding?.loadingIndicator?.visibility = View.GONE
                binding?.saveRecipeButton?.isEnabled = true
                return@setOnClickListener
            }

            val stepsContainer = binding?.stepsContainer
            stepsList.clear()
            stepsContainer?.let { container ->
                for (i in 0 until container.childCount) {
                    val child = container.getChildAt(i)
                    val editText = child.findViewById<EditText>(R.id.step_edit_text)
                    if (editText != null) {
                        val text = editText.text.toString().trim()
                        if (text.isNotEmpty()) stepsList.add(text)
                    }
                }
            }

            val imageUrl: String? = null

            val time = 30
            val difficulty = "Medium"
            val dietRestrictions = listOf<String>()

            val recipeId: String = recipeName.replace(" ", "_").lowercase()

            val descriptionText = binding?.descriptionEditText?.text?.toString()?.trim()
            val descriptionValue = if (descriptionText.isNullOrBlank()) null else descriptionText

            // Determine publisher email and uid (prefer FirebaseAuth current user, else stored prefs)
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val publisherEmail = firebaseUser?.email ?: run {
                val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                prefs.getString("email", null)
            }
            val publisherUid = firebaseUser?.uid ?: run {
                val prefs = requireContext().getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                prefs.getString("uid", null)
            }

            val recipe = Recipe(
                id = recipeId,
                name = recipeName,
                isFavorite = false,
                imageUrlString = imageUrl,
                publisher = publisherEmail,
                publisherId = publisherUid,
                ingredients = ingredientsList.toList(),
                steps = stepsList.toList(),
                time = time,
                difficulty = difficulty,
                dietRestrictions = dietRestrictions,
                description = descriptionValue
            )

            Model.shared.addRecipe(recipe) {
                // Ensure UI updates happen on main thread and hide loading indicator before dismiss
                activity?.runOnUiThread {
                    binding?.loadingIndicator?.visibility = View.GONE
                    binding?.saveRecipeButton?.isEnabled = true
                    dismiss()
                }
            }
        }

        binding?.selectImageButton?.setOnClickListener {
            // TODO: Implement image selection functionality (open gallery / camera)
        }
    }

    private fun addStepEditText(initialText: String) {
        val inflater = LayoutInflater.from(requireContext())
        val row = inflater.inflate(R.layout.row_step, binding?.stepsContainer, false)
        val editText = row.findViewById<EditText>(R.id.step_edit_text)
        val remove = row.findViewById<TextView>(R.id.remove_step_text)
        editText.setText(initialText)
        remove.setOnClickListener {
            binding?.stepsContainer?.removeView(row)
        }
        binding?.stepsContainer?.addView(row)
    }

    private fun showAddIngredientDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_add_ingredient, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.ingredient_name_edit_text)
        val amountInput = dialogView.findViewById<EditText>(R.id.ingredient_amount_edit_text)
        val unitInput = dialogView.findViewById<EditText>(R.id.ingredient_unit_edit_text)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.ingredient_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.button_add)) { dialogInterface, _ ->
                val name = nameInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull()
                val unit = unitInput.text.toString().trim().ifEmpty { null }
                if (name.isNotEmpty()) {
                    val ingredient = Ingredient(name = name, amount = amount, unit = unit)
                    ingredientsList.add(ingredient)
                    addIngredientRow(ingredient)
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton(getString(R.string.button_cancel)) { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun addIngredientRow(ingredient: Ingredient) {
        val inflater = LayoutInflater.from(requireContext())
        val row = inflater.inflate(R.layout.row_ingredient, binding?.ingredientsContainer, false)
        val text = row.findViewById<TextView>(R.id.ingredient_text)
        val remove = row.findViewById<TextView>(R.id.remove_ingredient_text)
        text.text = listOfNotNull(ingredient.amount?.toString(), ingredient.unit, ingredient.name).joinToString(" ")
        remove.setOnClickListener {
            binding?.ingredientsContainer?.removeView(row)
            ingredientsList.remove(ingredient)
        }
        binding?.ingredientsContainer?.addView(row)
    }

    private fun dismiss() {
        view?.findNavController()?.popBackStack()
    }
}