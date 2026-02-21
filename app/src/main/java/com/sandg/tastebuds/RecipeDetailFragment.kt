package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.sandg.tastebuds.databinding.FragmentRecipeDetailBinding
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import com.squareup.picasso.Picasso

class RecipeDetailFragment : Fragment() {

    private val sharedVm: SharedRecipesViewModel by activityViewModels()

    private var binding: FragmentRecipeDetailBinding? = null
    private var recipeId: String? = null
    private var recipe: Recipe? = null

    private var lastLocalUpdateTime: Long = 0L
    private var lastFetchStartTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recipeId = it.getString("recipeId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)

        val id = recipeId
        if (id.isNullOrEmpty()) {
            binding?.nameTextView?.text = "Recipe not found"
            return binding?.root
        }

        // First, try to get the recipe from SharedViewModel (includes favorite state)
        val existingRecipe = sharedVm.recipes.value?.firstOrNull { it.id == id }
        if (existingRecipe != null) {
            recipe = existingRecipe
            bindRecipe(existingRecipe)
        } else {
            // If not in ViewModel yet, show loading
            binding?.nameTextView?.text = "Loading..."
        }

        // Observe shared viewmodel so detail updates when favorites change elsewhere
        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            val idLocal = recipeId
            if (idLocal == null) return@observe
            val updated = list.firstOrNull { it.id == idLocal }
            if (updated != null) {
                recipe = updated
                bindRecipe(updated)
            }
        }

        // Also fetch from Model to ensure we have the latest data (in background)
        lastFetchStartTime = System.currentTimeMillis()
        Model.shared.getRecipeById(id) { r ->
            activity?.runOnUiThread {
                if (lastLocalUpdateTime > lastFetchStartTime) return@runOnUiThread
                recipe = r
                bindRecipe(r)
            }
        }

        binding?.editButton?.setOnClickListener {
            recipe?.let { r ->
                val bundle = Bundle().apply {
                    putString("recipeId", r.id)
                    putString("recipeName", r.name)
                    putString("description", r.description)
                    putInt("time", r.time ?: 30)
                    putString("difficulty", r.difficulty)
                    putInt("difficultyRating", r.difficultyRating ?: 0)
                    putString("imageUrl", r.imageUrlString)
                    putStringArrayList("steps", ArrayList(r.steps))
                    // Ingredients need to be serialized
                    val ingredientsJson = Gson().toJson(r.ingredients)
                    putString("ingredientsJson", ingredientsJson)
                }
                findNavController().navigate(R.id.action_global_addRecipeFragment, bundle)
            }
        }

        return binding?.root
    }

    private fun bindRecipe(r: Recipe) {
        binding?.nameTextView?.text = r.name

        val timeText = r.time?.let { "$it minutes" } ?: "Not specified"
        binding?.timeTextView?.text = timeText

        // Show difficulty level (Easy / Medium / Hard)
        val difficultyText = r.difficulty?.takeIf { it.isNotBlank() } ?: "Not specified"
        binding?.difficultyTextView?.text = difficultyText

        binding?.descriptionTextView?.text = r.description ?: "No description available"


        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        // Setup user rating card (only for other people's recipes)
        if (!currentUid.isNullOrEmpty() && r.publisherId != currentUid) {
            binding?.ratingCard?.visibility = View.VISIBLE
            setupUserRating(r, currentUid)
        } else {
            binding?.ratingCard?.visibility = View.GONE
        }

        // Load image only if URL is provided
        if (!r.imageUrlString.isNullOrEmpty()) {
            binding?.imageView?.let { imageView ->
                Picasso.get()
                    .load(r.imageUrlString)
                    .into(imageView, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            binding?.imageCard?.visibility = View.VISIBLE
                        }
                        override fun onError(e: Exception?) {
                            binding?.imageCard?.visibility = View.GONE
                        }
                    })
            }
        } else {
            binding?.imageCard?.visibility = View.GONE
        }

        // Show edit button only for recipe owner
        if ((!currentEmail.isNullOrEmpty() && !r.publisher.isNullOrEmpty() && currentEmail == r.publisher) ||
            (!currentUid.isNullOrEmpty() && !r.publisherId.isNullOrEmpty() && currentUid == r.publisherId)) {
            binding?.editButton?.visibility = View.VISIBLE
        } else {
            binding?.editButton?.visibility = View.GONE
        }

        binding?.ingredientsContainer?.removeAllViews()
        r.ingredients.forEach { ing ->
            val ctx = context ?: return
            val tv = TextView(ctx)
            val amt = ing.amount?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() } ?: ""
            val unit = ing.unit ?: ""
            val text = if (amt.isNotEmpty() || unit.isNotEmpty()) "${ing.name} \u2014 $amt $unit".trim() else ing.name
            tv.text = text
            tv.setPadding(0, 6, 0, 6)
            binding?.ingredientsContainer?.addView(tv)
        }

        binding?.stepsContainer?.removeAllViews()
        r.steps.forEachIndexed { idx, step ->
            val ctx = context ?: return
            val tv = TextView(ctx)
            tv.text = "${idx + 1}. $step"
            tv.setPadding(0, 6, 0, 6)
            binding?.stepsContainer?.addView(tv)
        }
    }

    private fun setupUserRating(r: Recipe, currentUid: String) {
        // Display average rating
        val avgRating = r.getAverageRating()
        val ratingCount = r.getRatingCount()
        binding?.averageRatingText?.text = String.format("%.1f", avgRating)
        binding?.ratingCountText?.text = "($ratingCount ${if (ratingCount == 1) "rating" else "ratings"})"

        // Get current user's rating
        val userRating = r.userRatings[currentUid] ?: 0

        // Setup star click listeners
        val stars = listOf(
            binding?.userRatingStar1,
            binding?.userRatingStar2,
            binding?.userRatingStar3,
            binding?.userRatingStar4,
            binding?.userRatingStar5
        )

        stars.forEachIndexed { index, star ->
            // Set initial state
            if (index < userRating) {
                star?.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                star?.setImageResource(android.R.drawable.btn_star_big_off)
            }

            // Set click listener
            star?.setOnClickListener {
                val newRating = index + 1
                saveUserRating(r, currentUid, newRating, stars)
            }
        }
    }

    private fun saveUserRating(r: Recipe, userId: String, rating: Int, stars: List<android.widget.ImageView?>) {
        // Update stars visually
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star?.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                star?.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }

        // Update recipe with new rating
        val updatedRatings = r.userRatings.toMutableMap()
        updatedRatings[userId] = rating

        val updatedRecipe = r.copy(userRatings = updatedRatings)

        // Save to Firebase
        Model.shared.addRecipe(updatedRecipe) {
            activity?.runOnUiThread {
                // Update local recipe and display
                recipe = updatedRecipe
                val avgRating = updatedRecipe.getAverageRating()
                val ratingCount = updatedRecipe.getRatingCount()
                binding?.averageRatingText?.text = String.format("%.1f", avgRating)
                binding?.ratingCountText?.text = "($ratingCount ${if (ratingCount == 1) "rating" else "ratings"})"

                showStyledToast("Rating saved!", android.R.drawable.ic_menu_save, true)
            }
        }
    }
}
