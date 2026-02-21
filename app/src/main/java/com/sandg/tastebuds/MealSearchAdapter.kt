package com.sandg.tastebuds

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.api.MealSummary
import com.squareup.picasso.Picasso

class MealSearchAdapter(
    private var meals: List<MealSummary> = emptyList(),
    private val onMealClick: (MealSummary) -> Unit
) : RecyclerView.Adapter<MealSearchAdapter.MealViewHolder>() {

    fun updateMeals(newMeals: List<MealSummary>) {
        meals = newMeals
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_search, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(meals[position])
    }

    override fun getItemCount() = meals.size

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mealThumb: ImageView = itemView.findViewById(R.id.meal_thumb)
        private val mealName: TextView = itemView.findViewById(R.id.meal_name)

        fun bind(meal: MealSummary) {
            mealName.text = meal.name
            Picasso.get()
                .load(meal.thumbUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(mealThumb)

            itemView.setOnClickListener { onMealClick(meal) }
        }
    }
}

