package com.sandg.tastebuds

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.databinding.RecipeRowLayoutBinding
import com.sandg.tastebuds.models.Recipe
import com.sandg.tastebuds.models.Model
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

        // favorite toggle (heart image) - animate on user click
        binding.favoriteImage.setOnClickListener {
            recipe?.let { r ->
                val toggled = r.copy(isFavorite = !r.isFavorite)

                // animate toggle (optimistic UI)
                animateFavoriteToggle(toggled.isFavorite)

                // Persist change
                Model.shared.addRecipe(toggled) {
                    // refresh UI based on saved value
                    bind(toggled)
                }
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

        // Set static icon (no animation) for normal bind
        updateFavoriteIcon(recipe.isFavorite)

        Log.v("TAG", "Loading image from URL: ${recipe.imageUrlString}")
        Picasso
            .get()
            .load(recipe.imageUrlString)
            .placeholder(R.drawable.avatar)
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
        // Prepare image state change and run a small bounce animation
        val iv = binding.favoriteImage
        // cancel any running animation
        iv.animate().cancel()

        // swap drawable immediately so animation shows filled heart when favoriting
        iv.setImageResource(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)

        // start scale-up then scale-down for a pop effect
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