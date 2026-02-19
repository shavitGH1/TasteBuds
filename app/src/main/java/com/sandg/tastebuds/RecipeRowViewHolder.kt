package com.sandg.tastebuds

import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.databinding.RecipeRowLayoutBinding
import com.sandg.tastebuds.models.Recipe
import com.squareup.picasso.Picasso

class RecipeRowViewHolder(
    private val binding: RecipeRowLayoutBinding,
    private val listener: OnItemClickListener?
): RecyclerView.ViewHolder(binding.root) {

    private var recipe: Recipe? = null

    init {
        itemView.setOnClickListener {
            recipe?.let { recipe ->
                listener?.onRecipeItemClick(recipe)
            }
        }

        binding.favoriteImage.setOnClickListener {
            recipe?.let { r ->
                val toggled = r.copy(isFavorite = !r.isFavorite)

                // Optimistic UI update
                recipe = toggled
                updateFavoriteIcon(toggled.isFavorite)
                animateFavoriteToggle(toggled.isFavorite)

                // Forward the action to the fragment/viewmodel with the desired state
                listener?.onToggleFavorite(toggled)
            }
        }

        binding.optionsButton.setOnClickListener { view ->
            recipe?.let { r ->
                listener?.onRecipeOptions(r, view)
            }
        }
    }

    fun bind(recipe: Recipe) {
        this.recipe = recipe
        binding.nameTextView.text = recipe.name

        // Display rating
        val avgRating = recipe.getAverageRating()
        val ratingCount = recipe.getRatingCount()
        if (ratingCount > 0) {
            binding.ratingTextView.text = String.format("%.1f", avgRating)
            binding.ratingCountTextView.text = "($ratingCount)"
        } else {
            binding.ratingTextView.text = "0.0"
            binding.ratingCountTextView.text = "(0)"
        }

        // Display difficulty rating (your score)
        if (recipe.difficultyRating != null && recipe.difficultyRating > 0) {
            val stars = "★".repeat(recipe.difficultyRating) + "☆".repeat(5 - recipe.difficultyRating)
            binding.difficultyRatingTextView.text = "Your: $stars"
            binding.difficultySeparator.visibility = android.view.View.VISIBLE
            binding.difficultyRatingTextView.visibility = android.view.View.VISIBLE
        } else {
            binding.difficultySeparator.visibility = android.view.View.GONE
            binding.difficultyRatingTextView.visibility = android.view.View.GONE
        }

        val timeText = recipe.time?.let { "$it min" } ?: ""
        val difficultyText = recipe.difficulty ?: ""
        binding.metaTextView.text = listOf(timeText, difficultyText).filter { it.isNotEmpty() }.joinToString(" • ")

        updateFavoriteIcon(recipe.isFavorite)

        if (!recipe.imageUrlString.isNullOrEmpty()) {
            Picasso
                .get()
                .load(recipe.imageUrlString)
                .into(binding.imageView)
        } else {
            binding.imageView.setImageDrawable(null)
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.favoriteImage.setImageResource(R.drawable.ic_favorite_filled)
            binding.favoriteImage.alpha = 1.0f
        } else {
            binding.favoriteImage.setImageResource(R.drawable.ic_favorite_border)
            binding.favoriteImage.alpha = 0.9f
        }
    }

    private fun animateFavoriteToggle(isFavorite: Boolean) {
        val iv = binding.favoriteImage
        iv.animate().cancel()

        iv.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)

        iv.scaleX = 0.8f
        iv.scaleY = 0.8f
        iv.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(140)
            .withEndAction {
                iv.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(140)
                    .start()
            }
            .start()
    }
}