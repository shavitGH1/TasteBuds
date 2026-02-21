package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.databinding.FragmentRecipeDetailBinding
import com.sandg.tastebuds.models.Recipe
import com.squareup.picasso.Picasso

class RecipeDetailFragment : Fragment() {

    private val sharedVm: SharedRecipesViewModel by activityViewModels()
    private val viewModel: RecipeDetailViewModel by viewModels()

    private var binding: FragmentRecipeDetailBinding? = null
    private var recipeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recipeId = arguments?.getString("recipeId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        val id = recipeId

        if (id.isNullOrEmpty()) {
            binding?.nameTextView?.text = getString(R.string.label_recipe_not_found)
            return binding?.root
        }

        setupEditButton()
        observeRecipe()

        // Seed the detail VM from the shared list first (instant), then fetch fresh copy
        val cached = sharedVm.recipes.value?.firstOrNull { it.id == id }
        if (cached != null) viewModel.setRecipe(cached)
        viewModel.loadRecipe(id)

        return binding?.root
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeRecipe() {
        viewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            if (recipe != null) bindRecipe(recipe)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading && viewModel.recipe.value == null) binding?.nameTextView?.text = getString(R.string.label_loading)
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private fun bindRecipe(r: Recipe) {
        binding?.nameTextView?.text = r.name
        binding?.timeTextView?.text = r.time?.let { getString(R.string.label_time_minutes, it) } ?: getString(R.string.label_not_specified)
        binding?.difficultyTextView?.text = r.difficulty?.takeIf { it.isNotBlank() } ?: getString(R.string.label_not_specified)
        binding?.descriptionTextView?.text = r.description ?: getString(R.string.label_no_description)

        loadImage(r.imageUrlString)
        showEditButtonIfOwner(r)
        setupRatingCard(r)
        populateIngredients(r)
        populateSteps(r)
    }

    private fun loadImage(url: String?) {
        if (url.isNullOrEmpty()) { binding?.imageCard?.visibility = View.GONE; return }
        binding?.imageView?.let { iv ->
            Picasso.get().load(url).into(iv, object : com.squareup.picasso.Callback {
                override fun onSuccess() { binding?.imageCard?.visibility = View.VISIBLE }
                override fun onError(e: Exception?) { binding?.imageCard?.visibility = View.GONE }
            })
        }
    }

    private fun showEditButtonIfOwner(r: Recipe) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val isOwner = !uid.isNullOrEmpty() && uid == r.publisherId
        binding?.editButton?.visibility = if (isOwner) View.VISIBLE else View.GONE
    }

    private fun setupEditButton() {
        binding?.editButton?.setOnClickListener {
            val r = viewModel.recipe.value ?: return@setOnClickListener
            findNavController().navigate(
                R.id.action_global_addRecipeFragment,
                Bundle().apply {
                    putString("recipeId", r.id)
                    putString("recipeName", r.name)
                    putString("description", r.description)
                    putInt("time", r.time ?: 30)
                    putString("difficulty", r.difficulty)
                    putString("imageUrl", r.imageUrlString)
                    putStringArrayList("steps", ArrayList(r.steps))
                    putString("ingredientsJson", com.google.gson.Gson().toJson(r.ingredients))
                }
            )
        }
    }

    private fun setupRatingCard(r: Recipe) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val isOtherUser = !uid.isNullOrEmpty() && r.publisherId != uid
        binding?.ratingCard?.visibility = if (isOtherUser) View.VISIBLE else View.GONE
        if (!isOtherUser) return

        val avg = r.getAverageRating()
        val count = r.getRatingCount()
        binding?.averageRatingText?.text = String.format(java.util.Locale.getDefault(), "%.1f", avg)
        binding?.ratingCountText?.text = if (count == 1)
            getString(R.string.label_ratings_count, count)
        else
            getString(R.string.label_ratings_count_plural, count)

        val stars = listOf(binding?.userRatingStar1, binding?.userRatingStar2, binding?.userRatingStar3, binding?.userRatingStar4, binding?.userRatingStar5)
        val userRating = r.userRatings[uid] ?: 0
        updateStarUI(stars, userRating)
        stars.forEachIndexed { index, star ->
            star?.setOnClickListener { onStarClicked(r, uid!!, index + 1, stars) }
        }
    }

    private fun onStarClicked(r: Recipe, uid: String, rating: Int, stars: List<ImageView?>) {
        updateStarUI(stars, rating)
        viewModel.saveRating(r, uid, rating)
        showStyledToast("Rating saved!", android.R.drawable.ic_menu_save, true)
    }

    private fun updateStarUI(stars: List<ImageView?>, rating: Int) {
        stars.forEachIndexed { i, star ->
            star?.setImageResource(if (i < rating) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        }
    }

    private fun populateIngredients(r: Recipe) {
        binding?.ingredientsContainer?.removeAllViews()
        r.ingredients.forEach { ing ->
            val tv = TextView(context ?: return)
            val amt = ing.amount?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() } ?: ""
            val unit = ing.unit ?: ""
            tv.text = if (amt.isNotEmpty() || unit.isNotEmpty()) "${ing.name} — $amt $unit".trim() else ing.name
            tv.setPadding(0, 6, 0, 6)
            binding?.ingredientsContainer?.addView(tv)
        }
    }

    private fun populateSteps(r: Recipe) {
        binding?.stepsContainer?.removeAllViews()
        r.steps.forEachIndexed { i, step ->
            val tv = TextView(context ?: return)
            tv.text = getString(R.string.step_number_format, i + 1, step)
            tv.setPadding(0, 6, 0, 6)
            binding?.stepsContainer?.addView(tv)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
