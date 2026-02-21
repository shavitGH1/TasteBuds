package com.sandg.tastebuds.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.sandg.tastebuds.models.FirebaseModel
import kotlinx.coroutines.tasks.await

/** Handles all authentication and user-document operations. No passwords stored in prefs. */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firebaseModel = FirebaseModel()

    /** Returns the username on success, or throws on failure. */
    suspend fun register(username: String, email: String, password: String): String {
        // Create Firebase Auth user
        val result = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (_: FirebaseAuthUserCollisionException) {
            // Email already registered — fall through to sign-in
            return signIn(email, password)
        }

        val user = result.user ?: throw IllegalStateException("No user after registration")
        val uid = user.uid

        // Check username uniqueness
        if (firebaseModel.isUsernameTakenSync(username)) {
            user.delete().await()
            throw IllegalArgumentException("Username already taken, choose another")
        }

        // Persist user document
        firebaseModel.createOrUpdateUserDocumentSync(uid, username, email)
        return username
    }

    /** Returns the username on success, or throws on failure. */
    suspend fun signIn(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Sign-in succeeded but no user")

        val doc = firebaseModel.getUserByIdSync(uid)
        return (doc?.get("name") as? String)?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
    }

    fun isSignedIn(): Boolean = auth.currentUser != null
}

