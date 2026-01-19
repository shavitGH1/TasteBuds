package com.sandg.tastebuds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.databinding.RecipeRowLayoutBinding
import com.sandg.tastebuds.models.Recipe

interface OnItemClickListener {
    fun onRecipeItemClick(recipe: Recipe)
    fun onToggleFavorite(recipe: Recipe)
}

class RecipesAdapter : ListAdapter<Recipe, RecipeRowViewHolder>(RecipeDiff()) {

    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeRowViewHolder {
        val inflator = LayoutInflater.from(parent.context)
        val binding = RecipeRowLayoutBinding.inflate(inflator, parent, false)
        return RecipeRowViewHolder(
            binding = binding,
            listener = listener
        )
    }

    override fun onBindViewHolder(holder: RecipeRowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeDiff : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem == newItem
    }
}