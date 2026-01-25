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
    }

    fun bind(recipe: Recipe) {
        this.recipe = recipe
        binding.nameTextView.text = recipe.name
        binding.authorTextView.text = recipe.publisher ?: ""

        val timeText = recipe.time?.let { "$it min" } ?: ""
        val difficultyText = recipe.difficulty ?: ""
        binding.metaTextView.text = listOf(timeText, difficultyText).filter { it.isNotEmpty() }.joinToString(" • ")

        updateFavoriteIcon(recipe.isFavorite)

        Picasso
            .get()
            .load(recipe.imageUrlString)
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(binding.imageView)
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