package com.sandg.tastebuds.models

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.base.Completion

class FirebaseAuthModel {

    private var auth: FirebaseAuth = Firebase.auth
    private val db = Firebase.firestore

    fun signIn(email: String, password: String, completion: Completion) {
        if (auth.currentUser != null) { completion(); return }

        // First try to sign in (for existing users)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    // Ensure a user doc exists in Firestore for visibility in console
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userDoc = mapOf("email" to email, "uid" to uid)
                        db.collection("users").document(uid).set(userDoc)
                            .addOnSuccessListener { }
                            .addOnFailureListener { _ -> }
                    }
                    completion()
                } else {
                    // If sign-in fails, try to create the user (auto-register)
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    val userDoc = mapOf("email" to email, "uid" to uid)
                                    db.collection("users").document(uid).set(userDoc)
                                        .addOnSuccessListener { }
                                        .addOnFailureListener { _ -> }
                                }
                            }
                            completion()
                        }
                        .addOnFailureListener { _ ->
                            completion()
                        }
                }
            }
            .addOnFailureListener { _ ->
                completion()
            }
    }
}
