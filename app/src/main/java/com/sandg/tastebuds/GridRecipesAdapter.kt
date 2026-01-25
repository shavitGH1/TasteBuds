package com.sandg.tastebuds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.sandg.tastebuds.models.Recipe

class GridRecipesAdapter : ListAdapter<Recipe, GridRecipesAdapter.GridViewHolder>(RecipeDiff()) {

    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val inflator = LayoutInflater.from(parent.context)
        val view = inflator.inflate(R.layout.recipe_grid_item, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GridViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val ivImage = view.findViewById<ImageView>(R.id.ivImage)
        private val tvName = view.findViewById<TextView>(R.id.tvName)
        private val tvPublisher = view.findViewById<TextView>(R.id.tvPublisher)
        private val fav = view.findViewById<ImageView>(R.id.favoriteImage)

        fun bind(recipe: Recipe) {
            tvName.text = recipe.name
            tvPublisher.text = recipe.publisher ?: ""
            Picasso.get().load(recipe.imageUrlString).placeholder(R.drawable.avatar).into(ivImage)

            // Update favorite icon based on recipe state
            fav.setImageResource(if (recipe.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)

            view.setOnClickListener { listener?.onRecipeItemClick(recipe) }

            // Toggle locally, send toggled recipe to listener for shared ViewModel handling
            fav.setOnClickListener {
                val toggled = recipe.copy(isFavorite = !recipe.isFavorite)
                // Optimistic UI update
                fav.setImageResource(if (toggled.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
                listener?.onToggleFavorite(toggled)
            }
        }
    }

    class RecipeDiff : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean = oldItem == newItem
    }
}
