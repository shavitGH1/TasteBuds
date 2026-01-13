package com.sandg.tastebuds

import android.util.Log
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
    }

    fun bind(recipe: Recipe, position: Int) {
        this.recipe = recipe
        binding.nameTextView.text = recipe.name
        binding.authorTextView.text = "Added by ${recipe.id}"
        Log.v("TAG", "Loading image from URL: ${recipe.imageUrlString}")
        Picasso
            .get()
            .load(recipe.imageUrlString)
            .into(binding.imageView)
    }
}