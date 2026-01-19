package com.sandg.tastebuds.models

import android.util.Log
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
                    Log.d("FB_AUTH", "signIn: success for $email")
                    // Ensure a user doc exists in Firestore for visibility in console
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userDoc = mapOf("email" to email, "uid" to uid)
                        db.collection("users").document(uid).set(userDoc)
                            .addOnSuccessListener { Log.d("FB_AUTH", "wrote user doc for $uid") }
                            .addOnFailureListener { e -> Log.e("FB_AUTH", "failed writing user doc", e) }
                    }
                    completion()
                } else {
                    Log.i("FB_AUTH", "signIn: failed (${signInTask.exception?.message}), trying to create user")
                    // If sign-in fails, try to create the user (auto-register)
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                Log.d("FB_AUTH", "createUser: success for $email")
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    val userDoc = mapOf("email" to email, "uid" to uid)
                                    db.collection("users").document(uid).set(userDoc)
                                        .addOnSuccessListener { Log.d("FB_AUTH", "wrote user doc for $uid") }
                                        .addOnFailureListener { e -> Log.e("FB_AUTH", "failed writing user doc", e) }
                                }
                            } else {
                                Log.e("FB_AUTH", "createUser: failed", createTask.exception)
                            }
                            completion()
                        }
                        .addOnFailureListener { e ->
                            Log.e("FB_AUTH", "createUser: failure", e)
                            completion()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FB_AUTH", "signInWithEmailAndPassword failed", e)
                completion()
            }
    }
}
