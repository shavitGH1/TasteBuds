package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.databinding.ActivityRegistrationBinding
import com.sandg.tastebuds.models.FirebaseModel

class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // ── Auto-login logic ──────────────────────────────────────────────────
        // 1) If Firebase already considers the user signed in, go straight to the app.
        val existingUser = FirebaseAuth.getInstance().currentUser
        if (existingUser != null) {
            navigateToMainActivity()
            return
        }

        // 2) If we have saved credentials, try a silent re-login.
        val prefs = getSharedPreferences("auth", MODE_PRIVATE)
        val savedEmail = prefs.getString("email", null)
        val savedPassword = prefs.getString("password", null)

        if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnSuccessListener {
                    // Successfully re-authenticated – go straight to main screen
                    navigateToMainActivity()
                }
                .addOnFailureListener {
                    // Credentials no longer valid – clear them and show the login form
                    prefs.edit { clear() }
                    showLoginForm()
                }
            // Don't inflate the UI yet; wait for the async result
            return
        }
        // ─────────────────────────────────────────────────────────────────────

        showLoginForm()
    }

    private fun showLoginForm() {
        val binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val switchMode = binding.switchMode
        val etUsername = binding.etUsername
        val etEmail = binding.etEmail
        val etPassword = binding.etPassword
        val btnNext = binding.btnGoToRecipes

        val auth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val firebaseModel = FirebaseModel()

        fun updateFieldsForMode(isRegister: Boolean) {
            etUsername.visibility = if (isRegister) View.VISIBLE else View.GONE
            etEmail.visibility = View.VISIBLE
            etPassword.visibility = View.VISIBLE
            etEmail.hint = if (isRegister) "Email" else "Email"
            btnNext.text = if (isRegister) getString(R.string.button_register) else getString(R.string.button_sign_in_username)
        }

        updateFieldsForMode(switchMode.isChecked)

        switchMode.setOnCheckedChangeListener { _, isChecked ->
            updateFieldsForMode(isChecked)
        }

        btnNext.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            val isRegister = switchMode.isChecked

            // For register mode, username is required
            if (isRegister && username.isEmpty()) {
                showStyledToast("Please enter a username")
                return@setOnClickListener
            }

            btnNext.isEnabled = false

            if (isRegister) {
                if (email.isEmpty() || password.isEmpty()) {
                    showStyledToast("Please enter email and password")
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showStyledToast("Please enter a valid email address")
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    showStyledToast("Password must be at least 6 characters")
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                // Create auth user FIRST, then check username (user needs to be authenticated to query Firestore)
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val user = auth.currentUser
                        val uid = user?.uid

                        if (uid != null) {
                            // Now check username availability (user is authenticated)
                            db.collection("users").whereEqualTo("name", username).get()
                                .addOnSuccessListener { snap ->
                                    if (!snap.isEmpty) {
                                        // Username taken - delete the auth user we just created
                                        user.delete()
                                        showStyledToast("Username already taken, choose another")
                                        btnNext.isEnabled = true
                                        return@addOnSuccessListener
                                    }

                                    // Username available - create user document
                                    firebaseModel.createOrUpdateUserDocument(uid, username, email) { success, err ->
                                        if (!success) {
                                            user.delete()
                                            if (err != null) showExceptionDialog(err) else showErrorDialog("Failed to write user document to Firestore.")
                                            btnNext.isEnabled = true
                                            return@createOrUpdateUserDocument
                                        }
                                        saveCredentialsAndContinue(username, email, uid, password)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    user.delete()
                                    showErrorDialog("Failed checking username availability: ${e.localizedMessage}")
                                    btnNext.isEnabled = true
                                }
                        } else {
                            showErrorDialog("Registration created but no user id returned.")
                            btnNext.isEnabled = true
                        }

                    }
                    .addOnFailureListener { createEx ->
                        if (createEx is FirebaseAuthUserCollisionException) {
                            // Email already registered - try to sign in
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    val user = auth.currentUser
                                    val uid = user?.uid
                                    if (uid != null) {
                                        firebaseModel.createOrUpdateUserDocument(uid, username, email) { success, err ->
                                            if (success) {
                                                saveCredentialsAndContinue(username, email, uid, password)
                                            } else {
                                                if (err != null) showExceptionDialog(err) else showErrorDialog("Failed to write user document after sign-in.")
                                                btnNext.isEnabled = true
                                            }
                                        }
                                    } else {
                                        saveCredentialsAndContinue(username, email, uid, password)
                                    }
                                }
                                .addOnFailureListener { signInEx ->
                                    showExceptionDialog(signInEx)
                                    btnNext.isEnabled = true
                                }
                        } else {
                            showExceptionDialog(createEx)
                            btnNext.isEnabled = true
                        }
                    }

            } else {
                // Login mode - use email directly
                if (email.isEmpty() || password.isEmpty()) {
                    showStyledToast("Please enter email and password")
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showStyledToast("Please enter a valid email address")
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener {
                        val user = auth.currentUser
                        val uid = user?.uid

                        if (uid != null) {
                            // Fetch the username from Firestore (now authenticated)
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { doc ->
                                    val storedUsername = doc.getString("name") ?: email.substringBefore("@")
                                    saveCredentialsAndContinue(storedUsername, email, uid, password)
                                }
                                .addOnFailureListener {
                                    // If we can't get username, use email prefix
                                    saveCredentialsAndContinue(email.substringBefore("@"), email, uid, password)
                                }
                        } else {
                            saveCredentialsAndContinue(email.substringBefore("@"), email, uid, password)
                        }
                    }
                    .addOnFailureListener { signInEx ->
                        showExceptionDialog(signInEx)
                        btnNext.isEnabled = true
                    }
            }
        }
    }

    private fun showErrorDialog(message: String) {
        showStyledToast(message, android.R.drawable.ic_dialog_alert)
    }

    private fun showExceptionDialog(t: Throwable) {
        val msg = t.localizedMessage ?: "An error occurred"
        showStyledToast(msg, android.R.drawable.ic_dialog_info)
    }

    private fun saveCredentialsAndContinue(username: String, email: String, uid: String?, password: String = "") {
        try {
            val prefs = getSharedPreferences("auth", MODE_PRIVATE)
            prefs.edit {
                putString("name", username)
                putString("email", email)
                putString("uid", uid)
                if (password.isNotEmpty()) putString("password", password)
            }
        } catch (_: Exception) {
        }

        showStyledToast("Welcome, $username!", android.R.drawable.ic_menu_send, true)
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
