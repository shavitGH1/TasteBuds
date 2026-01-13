package com.sandg.tastebuds

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.databinding.RecipeRowLayoutBinding
import com.sandg.tastebuds.models.Recipe

interface OnItemClickListener {
    fun onRecipeItemClick(recipe: Recipe)
}

class RecipesAdapter(
    private var recipes: List<Recipe>,
): RecyclerView.Adapter<RecipeRowViewHolder>() {

    var listener: OnItemClickListener? = null
    override fun getItemCount(): Int = recipes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeRowViewHolder {
        val inflator = LayoutInflater.from(parent.context)
        val binding = RecipeRowLayoutBinding.inflate(inflator, parent, false)
        return RecipeRowViewHolder(
            binding = binding,
            listener = listener
        )
    }

    override fun onBindViewHolder(holder: RecipeRowViewHolder, position: Int) {
        holder.bind(recipes[position], position)
    }
}