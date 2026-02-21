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

        binding.favoriteImage.visibility = android.view.View.GONE

        binding.optionsButton.setOnClickListener { view ->
            recipe?.let { r ->
                listener?.onRecipeOptions(r, view)
            }
        }
    }

    fun bind(recipe: Recipe) {
        this.recipe = recipe
        binding.nameTextView.text = recipe.name

        // Always show star rating under the name
        val avgRating = recipe.getAverageRating()
        val ratingCount = recipe.getRatingCount()
        binding.ratingTextView.visibility = android.view.View.VISIBLE
        binding.starSymbolTextView.visibility = android.view.View.VISIBLE
        binding.ratingCountTextView.visibility = android.view.View.VISIBLE
        if (ratingCount > 0) {
            binding.ratingTextView.text = String.format("%.1f", avgRating)
            binding.ratingCountTextView.text = "($ratingCount)"
        } else {
            binding.ratingTextView.text = "0.0"
            binding.ratingCountTextView.text = "(0)"
        }

        // Always show difficulty label when set
        val difficultyLabel = recipe.difficulty
        if (!difficultyLabel.isNullOrBlank()) {
            binding.difficultySeparator.visibility = android.view.View.VISIBLE
            binding.difficultyRatingTextView.text = difficultyLabel
            binding.difficultyRatingTextView.visibility = android.view.View.VISIBLE
        } else {
            binding.difficultySeparator.visibility = android.view.View.GONE
            binding.difficultyRatingTextView.visibility = android.view.View.GONE
        }

        val timeText = recipe.time?.let { "$it min" } ?: ""
        binding.metaTextView.text = timeText
        binding.metaTextView2.text = recipe.publisher ?: ""

        if (!recipe.imageUrlString.isNullOrEmpty()) {
            Picasso
                .get()
                .load(recipe.imageUrlString)
                .into(binding.imageView)
        } else {
            binding.imageView.setImageDrawable(null)
        }
    }
}