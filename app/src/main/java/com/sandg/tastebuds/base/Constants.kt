package com.sandg.tastebuds.base

import com.sandg.tastebuds.models.Recipe

typealias RecipesCompletion = (List<Recipe>) -> Unit
typealias RecipeCompletion = (Recipe) -> Unit
typealias Completion = () -> Unit