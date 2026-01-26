package com.sandg.tastebuds

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Recipe
import com.sandg.tastebuds.dao.AppLocalDB
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange

class SharedRecipesViewModel : ViewModel() {

    private val _recipes = MutableLiveData<List<Recipe>>(emptyList())
    val recipes: LiveData<List<Recipe>> = _recipes

    companion object {
        private const val TAG = "SharedRecipesVM"
    }

    private var lastAuthUid: String? = FirebaseAuth.getInstance().currentUser?.uid
    private var favListener: ListenerRegistration? = null

    // Auth state listener will trigger reload only when a user signs in or the UID changes.
    // We deliberately avoid reloading on sign-out to prevent clearing optimistic/local favorites from memory.
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val uid = auth.currentUser?.uid
        if (uid == lastAuthUid) return@AuthStateListener
        // unregister previous listener if any
        if (lastAuthUid != null) {
            try { favListener?.remove() } catch (_: Exception) { }
            favListener = null
        }
        lastAuthUid = uid
        if (!uid.isNullOrEmpty()) {
            // register a realtime listener for this user's favorites
            registerFavoritesListener(uid)
            // user signed in or changed user -> fetch remote favorites into local DB, apply them, then reload
            fetchAndApplyRemoteFavorites(uid) {
                // After remote favorites fetched into local DB, reload and re-apply
                reloadAll { applyFavoritesForUser(uid) }
            }
        }
    }

    init {
        // Initial load and apply favorites for current user when load completes
        val initialUid = FirebaseAuth.getInstance().currentUser?.uid
        if (!initialUid.isNullOrEmpty()) {
            registerFavoritesListener(initialUid)
        }
        reloadAll { if (!initialUid.isNullOrEmpty()) applyFavoritesForUser(initialUid) }
        // Listen for auth changes (login/logout) so we can re-apply per-user favorites when a user signs in
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        try { FirebaseAuth.getInstance().removeAuthStateListener(authStateListener) } catch (_: Exception) { }
        try { favListener?.remove() } catch (_: Exception) { }
        favListener = null
    }

    fun reloadAll(onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                val local = try {
                    AppLocalDB.db.recipeDao.getAllRecipes()
                } catch (e: Exception) {
                    emptyList<Recipe>()
                }

                Log.d(TAG, "reloadAll: local count=${local.size} ids=${local.map { it.id }}")

                // Load favorites for current user
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                val favoritesForUser = if (!currentUid.isNullOrEmpty()) {
                    try {
                        AppLocalDB.db.favoriteDao.getFavoritesForUser(currentUid).map { it.recipeId to it.isFavorite }.toMap()
                    } catch (e: Exception) {
                        emptyMap<String, Boolean>()
                    }
                } else emptyMap()

                // Grab any in-memory optimistic favorite flags so we don't overwrite them
                val inMemoryFavs = try { _recipes.value?.associate { it.id to it.isFavorite } ?: emptyMap() } catch (_: Exception) { emptyMap() }

                // Apply user-specific favorites to local list, preferring in-memory optimistic flags
                val localWithFavorites = local.map { r ->
                    val fav = inMemoryFavs[r.id] ?: favoritesForUser[r.id] ?: r.isFavorite
                    r.copy(isFavorite = fav)
                }

                // Only post local data immediately if we don't already have in-memory items (avoid overwriting optimistic UI)
                if ((_recipes.value == null || _recipes.value!!.isEmpty()) && localWithFavorites.isNotEmpty()) {
                    _recipes.postValue(localWithFavorites)
                }

                Model.shared.getAllRemoteRecipes { remoteList ->
                    Log.d(TAG, "reloadAll: remote count=${remoteList.size} ids=${remoteList.map { it.id }}")
                    try {
                        // Re-read favorites for current user in case they changed since we loaded `local` above
                        val freshFavoritesForUser = if (!currentUid.isNullOrEmpty()) {
                            try {
                                AppLocalDB.db.favoriteDao.getFavoritesForUser(currentUid).map { it.recipeId to it.isFavorite }.toMap()
                            } catch (e: Exception) {
                                emptyMap<String, Boolean>()
                            }
                        } else emptyMap()

                        // Grab any in-memory optimistic favorite flags from the live data so we don't overwrite recent toggles
                        val inMemoryFavs = try {
                            _recipes.value?.associate { it.id to it.isFavorite } ?: emptyMap()
                        } catch (e: Exception) { emptyMap<String, Boolean>() }

                        val map = mutableMapOf<String, Recipe>()
                        // start with remote but apply per-user and in-memory favorites to remote-only items
                        for (r in remoteList) {
                            val favForRemote = inMemoryFavs[r.id] ?: freshFavoritesForUser[r.id] ?: favoritesForUser[r.id] ?: r.isFavorite
                            map[r.id] = r.copy(isFavorite = favForRemote)
                        }
                        // merge local, preserving local data and per-user favorites
                        for (l in local) {
                            val existing = map[l.id]
                            if (existing != null) {
                                // keep server fields but apply user favorite with precedence:
                                // 1) in-memory optimistic favorite (user just toggled)
                                // 2) freshly read per-user favorites from DB
                                // 3) earlier favorites mapping loaded from DB
                                // 4) fallback to local recipe's flag
                                val fav = inMemoryFavs[l.id] ?: freshFavoritesForUser[l.id] ?: favoritesForUser[l.id] ?: l.isFavorite
                                val merged = existing.copy(
                                    isFavorite = fav,
                                    imageUrlString = existing.imageUrlString ?: l.imageUrlString,
                                    publisher = existing.publisher ?: l.publisher,
                                    publisherId = existing.publisherId ?: l.publisherId,
                                    ingredients = if (existing.ingredients.isNotEmpty()) existing.ingredients else l.ingredients,
                                    steps = if (existing.steps.isNotEmpty()) existing.steps else l.steps,
                                    time = existing.time ?: l.time,
                                    difficulty = existing.difficulty ?: l.difficulty,
                                    dietRestrictions = if (existing.dietRestrictions.isNotEmpty()) existing.dietRestrictions else l.dietRestrictions,
                                    description = existing.description ?: l.description
                                )
                                map[l.id] = merged
                            } else {
                                // use local, but apply favorites precedence similar to above
                                val fav = inMemoryFavs[l.id] ?: freshFavoritesForUser[l.id] ?: favoritesForUser[l.id] ?: l.isFavorite
                                map[l.id] = l.copy(isFavorite = fav)
                            }
                        }
                        val merged = map.values.toList()
                        Log.d(TAG, "reloadAll: merged count=${merged.size} ids=${merged.map { it.id }}")
                        _recipes.postValue(merged)
                    } catch (e: Exception) {
                        Log.e(TAG, "reloadAll: merge error", e)
                        // On error, try to preserve optimistic in-memory favorites and freshly read per-user favorites
                        val freshFavs = if (!currentUid.isNullOrEmpty()) {
                            try {
                                AppLocalDB.db.favoriteDao.getFavoritesForUser(currentUid).map { it.recipeId to it.isFavorite }.toMap()
                            } catch (_: Exception) { emptyMap<String, Boolean>() }
                        } else emptyMap()

                        val inMemoryFavsFallback = try { _recipes.value?.associate { it.id to it.isFavorite } ?: emptyMap() } catch (_: Exception) { emptyMap() }

                        val remoteWithFavs = remoteList.map { r ->
                            val fav = inMemoryFavsFallback[r.id] ?: freshFavs[r.id] ?: r.isFavorite
                            r.copy(isFavorite = fav)
                        }
                        _recipes.postValue(remoteWithFavs)
                     }

                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "reloadAll: unexpected error", e)
                Model.shared.getAllRemoteRecipes { remote ->
                    _recipes.postValue(remote)
                    onComplete?.invoke()
                }
            }
        }.start()
    }

    fun setRecipes(list: List<Recipe>) {
        _recipes.postValue(list)
    }

    fun refreshRecipe(id: String) {
        Model.shared.getRecipeById(id) { recipe ->
            val current = _recipes.value ?: emptyList()
            val idx = current.indexOfFirst { it.id == id }
            val newList = if (idx >= 0) {
                current.toMutableList().apply { this[idx] = recipe }
            } else {
                current + recipe
            }
            _recipes.postValue(newList)
        }
    }

    fun toggleFavorite(toggledRecipe: Recipe) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val updated = toggledRecipe.copy(isFavorite = toggledRecipe.isFavorite)

        // Update in-memory list optimistically
        val current = _recipes.value ?: emptyList()
        val newList = current.toMutableList()
        val idx = newList.indexOfFirst { it.id == updated.id }
        if (idx >= 0) newList[idx] = updated else newList.add(updated)
        _recipes.postValue(newList)

        // Persist per-user favorite locally and save the local recipe with the updated favorite flag.
        Thread {
            try {
                if (!currentUid.isNullOrEmpty()) {
                    if (updated.isFavorite) {
                        AppLocalDB.db.favoriteDao.upsert(com.sandg.tastebuds.models.Favorite(recipeId = updated.id, userId = currentUid, isFavorite = true))
                        Log.d(TAG, "toggleFavorite: upserted favorite for user=$currentUid recipe=${updated.id}")
                        // push to remote as well
                        try { pushFavoriteToRemote(currentUid, updated.id) } catch (e: Exception) { Log.e(TAG, "pushFavoriteToRemote failed", e) }
                    } else {
                        AppLocalDB.db.favoriteDao.deleteFavorite(updated.id, currentUid)
                        Log.d(TAG, "toggleFavorite: deleted favorite for user=$currentUid recipe=${updated.id}")
                        try { deleteFavoriteFromRemote(currentUid, updated.id) } catch (e: Exception) { Log.e(TAG, "deleteFavoriteFromRemote failed", e) }
                     }
                }
            } catch (e: Exception) {
                Log.e(TAG, "toggleFavorite: local persist failed", e)
            }

            // Save the recipe locally so the DB copy reflects the user's favorite state
            try {
                AppLocalDB.db.recipeDao.insertRecipes(updated.copy(isFavorite = updated.isFavorite))
            } catch (e: Exception) {
                Log.e(TAG, "toggleFavorite: save recipe local failed", e)
            }
            // No immediate remote reload here — rely on optimistic UI + local persistence to keep the favorite stable.
            // After persisting favorite state locally, ensure in-memory recipes reflect DB favorites for this user
            Log.d(TAG, "toggleFavorite: applying favorites after persist for user=$currentUid")
            currentUid?.let { applyFavoritesForUser(it) }
        }.start()
    }

    // --- Firestore helpers ---
    private val fStore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private fun pushFavoriteToRemote(uid: String, recipeId: String) {
        try {
            val docRef = fStore.collection("users").document(uid).collection("favorites").document(recipeId)
            val payload = mapOf("isFavorite" to true, "updatedAt" to FieldValue.serverTimestamp())
            docRef.set(payload).addOnSuccessListener {
                Log.d(TAG, "pushFavoriteToRemote: set remote favorite $recipeId for $uid")
            }.addOnFailureListener { e ->
                Log.e(TAG, "pushFavoriteToRemote failed", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushFavoriteToRemote exception", e)
        }
    }

    private fun deleteFavoriteFromRemote(uid: String, recipeId: String) {
        try {
            val docRef = fStore.collection("users").document(uid).collection("favorites").document(recipeId)
            docRef.delete().addOnSuccessListener {
                Log.d(TAG, "deleteFavoriteFromRemote: deleted remote favorite $recipeId for $uid")
            }.addOnFailureListener { e ->
                Log.e(TAG, "deleteFavoriteFromRemote failed", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteFavoriteFromRemote exception", e)
        }
    }

    private fun fetchAndApplyRemoteFavorites(uid: String, onComplete: (() -> Unit)? = null) {
        try {
            val col = fStore.collection("users").document(uid).collection("favorites")
            col.get().addOnSuccessListener { snapshot ->
                Thread {
                    try {
                        val favIds = snapshot.documents.mapNotNull { it.id }
                        val favSet = favIds.toSet()

                        // Upsert remote favorites into local DB
                        favIds.forEach { recipeId ->
                            try {
                                AppLocalDB.db.favoriteDao.upsert(com.sandg.tastebuds.models.Favorite(recipeId = recipeId, userId = uid, isFavorite = true))
                            } catch (e: Exception) { /* ignore per-item failures */ }
                        }

                        // Read local favorites for this user and push any local-only favorites to remote (two-way sync: union)
                        val localFavs = try { AppLocalDB.db.favoriteDao.getFavoritesForUser(uid).mapNotNull { it.recipeId } } catch (_: Exception) { emptyList<String>() }
                        val localOnly = localFavs.filter { it !in favSet }
                        localOnly.forEach { localId ->
                            try {
                                pushFavoriteToRemote(uid, localId)
                            } catch (e: Exception) { Log.e(TAG, "fetchAndApplyRemoteFavorites: push local-only failed for $localId", e) }
                        }

                        // Apply favorites to in-memory list
                        applyFavoritesForUser(uid)
                     } catch (e: Exception) {
                         Log.e(TAG, "fetchAndApplyRemoteFavorites: apply failed", e)
                     } finally {
                         onComplete?.invoke()
                     }
                 }.start()
             }.addOnFailureListener { e ->
                 Log.e(TAG, "fetchAndApplyRemoteFavorites failed", e)
                 onComplete?.invoke()
             }
         } catch (e: Exception) {
             Log.e(TAG, "fetchAndApplyRemoteFavorites exception", e)
             onComplete?.invoke()
         }
     }

    // Apply favorites for a specific user id (avoids relying on FirebaseAuth.currentUser during async operations)
    private fun applyFavoritesForUser(uid: String) {
        try {
            val favMap = try {
                AppLocalDB.db.favoriteDao.getFavoritesForUser(uid).associate { it.recipeId to it.isFavorite }
            } catch (e: Exception) {
                Log.e(TAG, "applyFavoritesForUser: failed to read favorites for user=$uid", e)
                emptyMap<String, Boolean>()
            }

            Log.d(TAG, "applyFavoritesForUser: user=$uid favCount=${favMap.size} favKeys=${favMap.keys}")

            val current = _recipes.value ?: emptyList()
            // Update existing entries
            val updated = current.map { r ->
                val fav = favMap[r.id]
                if (fav == null) r else r.copy(isFavorite = fav)
            }.toMutableList()

            // For favorites that refer to recipes not present in-memory, try to load them from local DB and add
            val missingIds = favMap.keys - updated.map { it.id }
            for (mid in missingIds) {
                try {
                    val recipe = try { AppLocalDB.db.recipeDao.getRecipeById(mid) } catch (_: Exception) { null }
                    if (recipe != null) {
                        updated.add(recipe.copy(isFavorite = favMap[mid] ?: recipe.isFavorite))
                    } else {
                        // Create a minimal placeholder so favorites persist in the in-memory list even if recipe details aren't available locally
                        updated.add(Recipe(id = mid, name = "", isFavorite = favMap[mid] ?: true))
                    }
                } catch (_: Exception) { /* ignore if not found */ }
            }

            _recipes.postValue(updated)
        } catch (e: Exception) {
            Log.e(TAG, "applyFavoritesForUser failed", e)
        }
    }

    // Register a realtime listener on the user's favorites subcollection to keep local DB in sync
    private fun registerFavoritesListener(uid: String) {
        try {
            favListener = fStore.collection("users").document(uid).collection("favorites")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "favorites listener error for user=$uid", error)
                        return@addSnapshotListener
                    }
                    if (snapshots == null) return@addSnapshotListener

                    Thread {
                        try {
                            for (dc in snapshots.documentChanges) {
                                val rid = dc.document.id
                                when (dc.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        try {
                                            AppLocalDB.db.favoriteDao.upsert(com.sandg.tastebuds.models.Favorite(recipeId = rid, userId = uid, isFavorite = true))
                                        } catch (e: Exception) { Log.e(TAG, "listener upsert failed", e) }
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        try {
                                            AppLocalDB.db.favoriteDao.deleteFavorite(rid, uid)
                                        } catch (e: Exception) { Log.e(TAG, "listener delete failed", e) }
                                    }
                                }
                            }
                            // After applying changes, update in-memory list
                            applyFavoritesForUser(uid)
                        } catch (e: Exception) {
                            Log.e(TAG, "favorites listener processing failed", e)
                        }
                    }.start()
                }
        } catch (e: Exception) {
            Log.e(TAG, "registerFavoritesListener failed", e)
        }
    }
}
