package com.sandg.tastebuds

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.databinding.FragmentRecipeDetailBinding
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import com.squareup.picasso.Picasso

class RecipeDetailFragment : Fragment() {

    private val sharedVm: SharedRecipesViewModel by activityViewModels()

    private var binding: FragmentRecipeDetailBinding? = null
    private var recipeId: String? = null
    private var recipe: Recipe? = null

    private var lastLocalUpdateTime: Long = 0L
    private var lastFetchStartTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recipeId = it.getString("recipeId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)

        val id = recipeId
        if (id.isNullOrEmpty()) {
            binding?.nameTextView?.text = "Recipe not found"
            return binding?.root
        }

        binding?.metaTextView?.text = "Loading..."

        lastFetchStartTime = System.currentTimeMillis()
        Model.shared.getRecipeById(id) { r ->
            activity?.runOnUiThread {
                if (lastLocalUpdateTime > lastFetchStartTime) return@runOnUiThread
                recipe = r
                bindRecipe(r)
            }
        }

        // Observe shared viewmodel so detail updates when favorites change elsewhere
        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            val idLocal = recipeId
            if (idLocal == null) return@observe
            val updated = list.firstOrNull { it.id == idLocal }
            if (updated != null) {
                recipe = updated
                bindRecipe(updated)
            }
        }

        binding?.favoriteFab?.setOnClickListener {
            recipe?.let { r ->
                val toggled = r.copy(isFavorite = !r.isFavorite)
                lastLocalUpdateTime = System.currentTimeMillis()
                recipe = toggled
                updateFavoriteIcon(toggled.isFavorite)
                animateFavoriteToggle(toggled.isFavorite)

                // Use shared ViewModel to toggle per-user favorite (avoids writing global favorite to server)
                sharedVm.toggleFavorite(toggled)
            }
        }

        binding?.editButton?.setOnClickListener {
            findNavController().navigate(R.id.action_global_addRecipeFragment)
        }

        return binding?.root
    }

    private fun bindRecipe(r: Recipe) {
        binding?.nameTextView?.text = r.name
        binding?.authorTextView?.text = r.publisher ?: ""

        val timeText = r.time?.let { "$it min" } ?: ""
        val difficultyText = r.difficulty ?: ""
        val dietText = if (r.dietRestrictions.isNotEmpty()) r.dietRestrictions.joinToString(", ") else ""
        val metaParts = listOf(timeText, difficultyText, dietText).filter { it.isNotEmpty() }
        binding?.metaTextView?.text = metaParts.joinToString(" • ")

        binding?.descriptionTextView?.text = r.description ?: ""

        updateFavoriteIcon(r.isFavorite)

        binding?.imageView?.let { imageView ->
            Picasso.get()
                .load(r.imageUrlString)
                .placeholder(R.drawable.ic_baseline_person_24)
                .into(imageView)
        }

        val currentEmail = FirebaseAuth.getInstance().currentUser?.email
        if (!currentEmail.isNullOrEmpty() && !r.publisher.isNullOrEmpty() && currentEmail == r.publisher) {
            binding?.editButton?.visibility = View.VISIBLE
        } else {
            binding?.editButton?.visibility = View.GONE
        }

        binding?.ingredientsContainer?.removeAllViews()
        r.ingredients.forEach { ing ->
            val tv = TextView(requireContext())
            val amt = ing.amount?.let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() } ?: ""
            val unit = ing.unit ?: ""
            val text = if (amt.isNotEmpty() || unit.isNotEmpty()) "${ing.name} — $amt $unit".trim() else ing.name
            tv.text = text
            tv.setPadding(0, 6, 0, 6)
            binding?.ingredientsContainer?.addView(tv)
        }

        binding?.stepsContainer?.removeAllViews()
        r.steps.forEachIndexed { idx, step ->
            val tv = TextView(requireContext())
            tv.text = "${idx + 1}. $step"
            tv.setPadding(0, 6, 0, 6)
            binding?.stepsContainer?.addView(tv)
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        binding?.favoriteFab?.let { fab ->
            fab.imageTintList = null
            if (isFavorite) {
                fab.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                fab.setImageResource(R.drawable.ic_favorite_border)
            }
        }
    }

    private fun animateFavoriteToggle(isFavorite: Boolean) {
        val fab = binding?.favoriteFab ?: return
        fab.imageTintList = null

        if (isFavorite) {
            val animDrawable = androidx.core.content.res.ResourcesCompat.getDrawable(resources, R.drawable.avd_heart_fill, requireContext().theme)
            if (animDrawable != null) {
                fab.setImageDrawable(animDrawable)
                if (animDrawable is Animatable) (animDrawable as Animatable).start()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (recipe?.isFavorite == true) {
                        fab.setImageResource(R.drawable.ic_favorite_filled)
                    }
                }, 220)
            }
        } else {
            fab.setImageResource(R.drawable.ic_favorite_border)
        }

        fab.scaleX = 0.85f
        fab.scaleY = 0.85f
        fab.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(160)
            .withEndAction {
                fab.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
            }
            .start()
    }
}
