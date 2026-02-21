package com.sandg.tastebuds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.models.Recipe
import com.squareup.picasso.Picasso

class RecommendationsAdapter(
    private val recipes: List<Recipe>,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecommendationsAdapter.RecommendationViewHolder>() {

    inner class RecommendationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image: ImageView = view.findViewById(R.id.recommendationImage)
        private val name: TextView = view.findViewById(R.id.recommendationName)

        fun bind(recipe: Recipe) {
            name.text = recipe.name

            if (!recipe.imageUrlString.isNullOrEmpty()) {
                Picasso.get()
                    .load(recipe.imageUrlString)
                    .into(image)
            } else {
                image.setImageDrawable(null)
            }

            itemView.setOnClickListener {
                onRecipeClick(recipe)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recommendation_item, parent, false)
        return RecommendationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount() = recipes.size
}

