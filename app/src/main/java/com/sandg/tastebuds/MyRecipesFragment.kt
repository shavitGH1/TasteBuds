package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.models.Recipe

class MyRecipesFragment : Fragment() {

    private val sharedVm: SharedRecipesViewModel by activityViewModels()
    private lateinit var adapter: RecipesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_my_recipes, container, false)
        val recycler = root.findViewById<RecyclerView>(R.id.recyclerView)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = RecipesAdapter()
        adapter.listener = object : OnItemClickListener {
            override fun onRecipeItemClick(recipe: Recipe) {
                val args = bundleOf("recipeId" to recipe.id)
                findNavController().navigate(R.id.action_recipesListFragment_to_recipeDetailFragment, args)
            }

            override fun onToggleFavorite(recipe: Recipe) {
                sharedVm.toggleFavorite(recipe)
            }
        }
        recycler.adapter = adapter

        sharedVm.recipes.observe(viewLifecycleOwner) { list ->
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val filtered = list.filter { r -> (r.publisherId == currentUid) || r.isFavorite }
            adapter.submitList(filtered)
        }

        return root
    }
}
