package com.sandg.tastebuds

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sandg.tastebuds.databinding.FragmentAddRecipeBinding
import com.sandg.tastebuds.models.Ingredient
import com.sandg.tastebuds.models.Recipe
import com.squareup.picasso.Picasso
import android.util.Patterns

class AddRecipeFragment : Fragment() {

    private var binding: FragmentAddRecipeBinding? = null
    private val viewModel: AddRecipeViewModel by viewModels()

    private val ingredientsList = mutableListOf<Ingredient>()
    private val stepsList = mutableListOf<String>()
    private var selectedDifficulty = "Medium"
    private var isEditMode = false
    private var editingRecipeId: String? = null
    private var existingRecipe: Recipe? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAddRecipeBinding.inflate(layoutInflater, container, false)
        parseArguments()
        setupView()
        observeSaveState()
        return binding?.root
    }

    // ── Argument parsing ──────────────────────────────────────────────────────

    private fun parseArguments() {
        arguments?.let { args ->
            editingRecipeId = args.getString("recipeId")
            isEditMode = editingRecipeId != null
            loadFormData(args)
        }
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupView() {
        setLoading(false)
        if (isEditMode) binding?.importFromWebButton?.visibility = View.GONE
        binding?.importFromWebButton?.setOnClickListener {
            view?.findNavController()?.navigate(R.id.action_global_mealSearchFragment)
        }
        setupDifficultyButtons()
        binding?.addStepButton?.setOnClickListener { addStepRow("") }
        binding?.addIngredientButton?.setOnClickListener { showAddIngredientDialog() }
        binding?.previewImageButton?.setOnClickListener { previewImage() }
        binding?.saveRecipeButton?.setOnClickListener { attemptSave() }
    }

    private fun observeSaveState() {
        viewModel.saveState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SaveRecipeState.Loading -> setLoading(true)
                is SaveRecipeState.Success -> {
                    setLoading(false)
                    showStyledToast(getString(R.string.success_recipe_saved), android.R.drawable.ic_menu_save, true)
                    findNavController().popBackStack()
                }
                is SaveRecipeState.Error -> {
                    setLoading(false)
                    showStyledToast(state.message)
                }
                else -> setLoading(false)
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun attemptSave() {
        val name = binding?.nameEditText?.text.toString().trim()
        val timeText = binding?.preparationTimeEditText?.text?.toString()?.trim()
        val imageUrl = binding?.imageUrlEditText?.text?.toString()?.trim()

        if (!validateForm(name, timeText, imageUrl)) return

        collectSteps()
        val time = timeText!!.toInt()

        viewModel.saveRecipe(
            existingId = editingRecipeId,
            name = name,
            description = binding?.descriptionEditText?.text?.toString()?.trim(),
            imageUrl = imageUrl,
            time = time,
            difficulty = selectedDifficulty,
            ingredients = ingredientsList.toList(),
            steps = stepsList.toList(),
            existingRecipe = existingRecipe
        )
    }

    private fun validateForm(name: String, timeText: String?, imageUrl: String?): Boolean {
        if (name.isEmpty()) { showStyledToast(getString(R.string.error_recipe_name_required)); return false }
        val time = timeText?.toIntOrNull()
        if (timeText.isNullOrEmpty() || time == null || time <= 0) { showStyledToast(getString(R.string.error_invalid_time)); return false }
        if (!imageUrl.isNullOrEmpty() && !isValidUrl(imageUrl)) { showStyledToast(getString(R.string.error_invalid_url)); return false }
        if (ingredientsList.isEmpty()) { showStyledToast(getString(R.string.error_no_ingredients)); return false }
        collectSteps()
        if (stepsList.isEmpty()) { showStyledToast(getString(R.string.error_no_steps)); return false }
        return true
    }

    private fun collectSteps() {
        stepsList.clear()
        val container = binding?.stepsContainer ?: return
        for (i in 0 until container.childCount) {
            val et = container.getChildAt(i).findViewById<EditText>(R.id.step_edit_text)
            val text = et?.text?.toString()?.trim()
            if (!text.isNullOrEmpty()) stepsList.add(text)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        binding?.loadingIndicator?.visibility = if (loading) View.VISIBLE else View.GONE
        binding?.saveRecipeButton?.isEnabled = !loading
    }

    private fun isValidUrl(url: String) =
        Patterns.WEB_URL.matcher(url).matches() &&
        (url.startsWith("http://", true) || url.startsWith("https://", true))

    private fun previewImage() {
        val url = binding?.imageUrlEditText?.text?.toString()?.trim()
        if (!url.isNullOrEmpty() && isValidUrl(url)) loadImagePreview(url)
        else showStyledToast(getString(R.string.error_invalid_url))
    }

    private fun loadImagePreview(url: String) {
        binding?.avatarImageView?.let { iv ->
            Picasso.get().load(url).into(iv, object : com.squareup.picasso.Callback {
                override fun onSuccess() { iv.visibility = View.VISIBLE }
                override fun onError(e: Exception?) { iv.visibility = View.GONE; showStyledToast("Failed to load image") }
            })
        }
    }

    private fun setupDifficultyButtons() {
        updateDifficultyUI()
        binding?.difficultyEasyButton?.setOnClickListener { selectedDifficulty = "Easy"; updateDifficultyUI() }
        binding?.difficultyMediumButton?.setOnClickListener { selectedDifficulty = "Medium"; updateDifficultyUI() }
        binding?.difficultyHardButton?.setOnClickListener { selectedDifficulty = "Hard"; updateDifficultyUI() }
    }

    private fun updateDifficultyUI() {
        val selected = requireContext().getColor(android.R.color.holo_green_dark)
        val default = requireContext().getColor(android.R.color.darker_gray)
        listOf(
            binding?.difficultyEasyButton to "Easy",
            binding?.difficultyMediumButton to "Medium",
            binding?.difficultyHardButton to "Hard"
        ).forEach { (btn, level) ->
            val isSelected = selectedDifficulty == level
            btn?.setTextColor(if (isSelected) selected else default)
            btn?.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
    }

    private fun addStepRow(text: String) {
        val row = LayoutInflater.from(requireContext()).inflate(R.layout.row_step, binding?.stepsContainer, false)
        row.findViewById<EditText>(R.id.step_edit_text).setText(text)
        row.findViewById<TextView>(R.id.remove_step_text).setOnClickListener { binding?.stepsContainer?.removeView(row) }
        binding?.stepsContainer?.addView(row)
    }

    private fun showAddIngredientDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ingredient, null)
        val nameInput = view.findViewById<EditText>(R.id.ingredient_name_edit_text)
        val amountInput = view.findViewById<EditText>(R.id.ingredient_amount_edit_text)
        val unitInput = view.findViewById<EditText>(R.id.ingredient_unit_edit_text)
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.ingredient_dialog_title))
            .setView(view)
            .setPositiveButton(getString(R.string.button_add)) { d, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    val ing = Ingredient(name, amountInput.text.toString().toDoubleOrNull(), unitInput.text.toString().trim().ifEmpty { null })
                    ingredientsList.add(ing); addIngredientRow(ing)
                }
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.button_cancel)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun addIngredientRow(ing: Ingredient) {
        val row = LayoutInflater.from(requireContext()).inflate(R.layout.row_ingredient, binding?.ingredientsContainer, false)
        row.findViewById<TextView>(R.id.ingredient_text).text =
            listOfNotNull(ing.amount?.toString(), ing.unit, ing.name).joinToString(" ")
        row.findViewById<TextView>(R.id.remove_ingredient_text).setOnClickListener {
            binding?.ingredientsContainer?.removeView(row); ingredientsList.remove(ing)
        }
        binding?.ingredientsContainer?.addView(row)
    }

    private fun loadFormData(args: Bundle) {
        args.getString("recipeName")?.let { binding?.nameEditText?.setText(it) }
        args.getString("description")?.let { binding?.descriptionEditText?.setText(it) }
        binding?.preparationTimeEditText?.setText(args.getInt("time", 30).toString())
        args.getString("difficulty")?.takeIf { it in listOf("Easy", "Medium", "Hard") }?.let {
            selectedDifficulty = it; updateDifficultyUI()
        }
        args.getString("imageUrl")?.let { url ->
            binding?.imageUrlEditText?.setText(url)
            if (url.isNotEmpty()) loadImagePreview(url)
        }
        args.getString("ingredientsJson")?.let { json ->
            runCatching {
                val type = object : TypeToken<List<Ingredient>>() {}.type
                val list: List<Ingredient> = Gson().fromJson(json, type)
                ingredientsList.clear(); list.forEach { ingredientsList.add(it); addIngredientRow(it) }
            }
        }
        args.getStringArrayList("steps")?.forEach { addStepRow(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}