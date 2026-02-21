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
import com.squareup.picasso.Picasso
import android.util.Patterns
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AddRecipeFragment : Fragment() {

    private var binding: FragmentAddRecipeBinding? = null

    private val ingredientsList = mutableListOf<Ingredient>()
    private val stepsList = mutableListOf<String>()

    // Edit mode variables
    private var isEditMode = false
    private var editingRecipeId: String? = null
    private var existingRecipe: Recipe? = null

    // Difficulty level selection
    private var selectedDifficulty: String = "Medium"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddRecipeBinding.inflate(layoutInflater, container, false)

        // Check if we're editing an existing recipe or importing from web
        arguments?.let { args ->
            editingRecipeId = args.getString("recipeId")
            if (editingRecipeId != null) {
                isEditMode = true
                loadRecipeData(args)
            } else if (args.getString("recipeName") != null) {
                // Coming from MealDB web import - pre-fill form but not in edit mode
                loadRecipeData(args)
            }
        }

        setupView()
        return binding?.root
    }

    private fun setupView() {

        binding?.loadingIndicator?.visibility = View.GONE

        // Import from Web button - navigate to MealSearchFragment
        binding?.importFromWebButton?.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_global_mealSearchFragment)
        }
        // Hide import card entirely in edit mode
        if (isEditMode) {
            binding?.importFromWebButton?.visibility = View.GONE
        }

        // Setup difficulty buttons
        setupDifficultyButtons()

        binding?.addStepButton?.setOnClickListener {
            addStepEditText("")
        }

        binding?.addIngredientButton?.setOnClickListener {
            showAddIngredientDialog()
        }

        // Preview image from URL
        binding?.previewImageButton?.setOnClickListener {
            val imageUrl = binding?.imageUrlEditText?.text?.toString()?.trim()
            if (!imageUrl.isNullOrEmpty() && isValidUrl(imageUrl)) {
                loadImagePreview(imageUrl)
            } else {
                showStyledToast(getString(R.string.error_invalid_url))
            }
        }

        binding?.saveRecipeButton?.setOnClickListener {
            saveRecipe()
        }
    }

    private fun saveRecipe() {
        // Prevent double-clicks while saving
        binding?.saveRecipeButton?.isEnabled = false
        binding?.loadingIndicator?.visibility = View.VISIBLE

        // Validate recipe name
        val recipeName: String = binding?.nameEditText?.text.toString().trim()
        if (recipeName.isEmpty()) {
            showStyledToast(getString(R.string.error_recipe_name_required))
            binding?.loadingIndicator?.visibility = View.GONE
            binding?.saveRecipeButton?.isEnabled = true
            return
        }

        // Validate preparation time (must be a number)
        val timeText = binding?.preparationTimeEditText?.text?.toString()?.trim()
        val time: Int? = timeText?.toIntOrNull()
        if (timeText.isNullOrEmpty() || time == null || time <= 0) {
            showStyledToast(getString(R.string.error_invalid_time))
            binding?.loadingIndicator?.visibility = View.GONE
            binding?.saveRecipeButton?.isEnabled = true
            return
        }

        // Validate image URL
        val imageUrl: String? = binding?.imageUrlEditText?.text?.toString()?.trim()
        if (!imageUrl.isNullOrEmpty() && !isValidUrl(imageUrl)) {
            showStyledToast(getString(R.string.error_invalid_url))
            binding?.loadingIndicator?.visibility = View.GONE
            binding?.saveRecipeButton?.isEnabled = true
            return
        }

        // Collect steps
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

        // Validate ingredients
        if (ingredientsList.isEmpty()) {
            showStyledToast(getString(R.string.error_no_ingredients))
            binding?.loadingIndicator?.visibility = View.GONE
            binding?.saveRecipeButton?.isEnabled = true
            return
        }

        // Validate steps
        if (stepsList.isEmpty()) {
            showStyledToast(getString(R.string.error_no_steps))
            binding?.loadingIndicator?.visibility = View.GONE
            binding?.saveRecipeButton?.isEnabled = true
            return
        }

        val difficulty = selectedDifficulty
        val dietRestrictions = listOf<String>()

        // Use existing recipe ID in edit mode, or create new one
        val recipeId: String = if (isEditMode && !editingRecipeId.isNullOrEmpty()) {
            editingRecipeId!!
        } else {
            "${System.currentTimeMillis()}_${recipeName.replace(" ", "_").lowercase()}"
        }

        val descriptionText = binding?.descriptionEditText?.text?.toString()?.trim()
        val descriptionValue = if (descriptionText.isNullOrBlank()) null else descriptionText

        // Determine publisher email and uid
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
            imageUrlString = if (imageUrl.isNullOrEmpty()) null else imageUrl,
            publisher = publisherEmail,
            publisherId = publisherUid,
            ingredients = ingredientsList.toList(),
            steps = stepsList.toList(),
            time = time,
            difficulty = difficulty,
            dietRestrictions = dietRestrictions,
            description = descriptionValue,
            difficultyRating = if (isEditMode && existingRecipe != null) existingRecipe!!.difficultyRating else null,
            userRatings = if (isEditMode && existingRecipe != null) existingRecipe!!.userRatings else mapOf()
        )

        Model.shared.addRecipe(recipe) {
            activity?.runOnUiThread {
                binding?.loadingIndicator?.visibility = View.GONE
                binding?.saveRecipeButton?.isEnabled = true
                showStyledToast(getString(R.string.success_recipe_saved), android.R.drawable.ic_menu_save, true)
                dismiss()
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url).matches() &&
               (url.startsWith("http://", ignoreCase = true) ||
                url.startsWith("https://", ignoreCase = true))
    }

    private fun setupDifficultyButtons() {
        updateDifficultyButtons()

        binding?.difficultyEasyButton?.setOnClickListener {
            selectedDifficulty = "Easy"
            updateDifficultyButtons()
        }
        binding?.difficultyMediumButton?.setOnClickListener {
            selectedDifficulty = "Medium"
            updateDifficultyButtons()
        }
        binding?.difficultyHardButton?.setOnClickListener {
            selectedDifficulty = "Hard"
            updateDifficultyButtons()
        }
    }

    private fun updateDifficultyButtons() {
        val selectedColor = requireContext().getColor(android.R.color.holo_green_dark)
        val defaultColor = requireContext().getColor(android.R.color.darker_gray)

        binding?.difficultyEasyButton?.setTextColor(
            if (selectedDifficulty == "Easy") selectedColor else defaultColor
        )
        binding?.difficultyMediumButton?.setTextColor(
            if (selectedDifficulty == "Medium") selectedColor else defaultColor
        )
        binding?.difficultyHardButton?.setTextColor(
            if (selectedDifficulty == "Hard") selectedColor else defaultColor
        )

        // Bold the selected one
        val bold = android.graphics.Typeface.BOLD
        val normal = android.graphics.Typeface.NORMAL
        binding?.difficultyEasyButton?.setTypeface(null, if (selectedDifficulty == "Easy") bold else normal)
        binding?.difficultyMediumButton?.setTypeface(null, if (selectedDifficulty == "Medium") bold else normal)
        binding?.difficultyHardButton?.setTypeface(null, if (selectedDifficulty == "Hard") bold else normal)
    }

    private fun loadImagePreview(url: String) {
        binding?.avatarImageView?.let { imageView ->
            Picasso.get()
                .load(url)
                .into(imageView, object : com.squareup.picasso.Callback {
                    override fun onSuccess() {
                        imageView.visibility = View.VISIBLE
                    }
                    override fun onError(e: Exception?) {
                        imageView.visibility = View.GONE
                        showStyledToast("Failed to load image. Please check the URL.")
                    }
                })
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

    private fun loadRecipeData(args: Bundle) {
        // Fetch existing recipe if we're editing
        if (isEditMode && !editingRecipeId.isNullOrEmpty()) {
            Model.shared.getRecipeById(editingRecipeId!!) { recipe ->
                activity?.runOnUiThread {
                    existingRecipe = recipe
                }
            }
        }

        // Load recipe name
        args.getString("recipeName")?.let { name ->
            binding?.nameEditText?.setText(name)
        }

        // Load description
        args.getString("description")?.let { desc ->
            binding?.descriptionEditText?.setText(desc)
        }

        // Load preparation time
        val time = args.getInt("time", 30)
        binding?.preparationTimeEditText?.setText(time.toString())

        // Load difficulty level
        args.getString("difficulty")?.let { diff ->
            if (diff in listOf("Easy", "Medium", "Hard")) {
                selectedDifficulty = diff
                updateDifficultyButtons()
            }
        }

        // Load image URL
        args.getString("imageUrl")?.let { url ->
            binding?.imageUrlEditText?.setText(url)
            if (url.isNotEmpty()) {
                loadImagePreview(url)
            }
        }

        // Load ingredients
        args.getString("ingredientsJson")?.let { json ->
            try {
                val type = object : TypeToken<List<Ingredient>>() {}.type
                val ingredients: List<Ingredient> = Gson().fromJson(json, type)
                ingredientsList.clear()
                ingredients.forEach { ingredient ->
                    ingredientsList.add(ingredient)
                    addIngredientRow(ingredient)
                }
            } catch (e: Exception) {
                // If parsing fails, just continue without ingredients
            }
        }

        // Load steps
        args.getStringArrayList("steps")?.let { steps ->
            steps.forEach { step ->
                addStepEditText(step)
            }
        }
    }
}