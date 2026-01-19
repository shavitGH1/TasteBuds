package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.databinding.ActivityRegistrationBinding
import com.sandg.tastebuds.models.FirebaseModel
import com.sandg.tastebuds.models.Recipe
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import android.view.animation.AnimationUtils

class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

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
            etEmail.visibility = if (isRegister) View.VISIBLE else View.GONE
            etPassword.visibility = View.VISIBLE
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

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnNext.isEnabled = false

            if (isRegister) {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                db.collection("users").whereEqualTo("name", username).get()
                    .addOnSuccessListener { snap ->
                        if (!snap.isEmpty) {
                            Toast.makeText(this, "Username already taken, choose another", Toast.LENGTH_LONG).show()
                            btnNext.isEnabled = true
                            return@addOnSuccessListener
                        }

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                val uid = user?.uid

                                if (uid != null) {
                                    firebaseModel.createOrUpdateUserDocument(uid, username, email) { success, err ->
                                        if (!success) {
                                            if (err != null) showExceptionDialog(err) else showErrorDialog("Failed to write user document to Firestore. Please check your Firestore rules and network.")
                                            btnNext.isEnabled = true
                                            return@createOrUpdateUserDocument
                                        }

                                        val r1 = Recipe(
                                            id = "sample_${uid}_1",
                                            name = "Sample Recipe 1",
                                            isFavorite = false,
                                            imageUrlString = null,
                                            publisher = username,
                                            publisherId = uid,
                                            ingredients = listOf(),
                                            steps = listOf("Do this", "Then that"),
                                            time = null,
                                            difficulty = null,
                                            dietRestrictions = listOf(),
                                            description = null
                                        )
                                        val r2 = Recipe(
                                            id = "sample_${uid}_2",
                                            name = "Sample Recipe 2",
                                            isFavorite = false,
                                            imageUrlString = null,
                                            publisher = username,
                                            publisherId = uid,
                                            ingredients = listOf(),
                                            steps = listOf("Mix", "Bake"),
                                            time = null,
                                            difficulty = null,
                                            dietRestrictions = listOf(),
                                            description = null
                                        )
                                        firebaseModel.addRecipe(r1) { }
                                        firebaseModel.addRecipe(r2) { }

                                        saveCredentialsAndContinue(username, email, uid)
                                    }
                                } else {
                                    showErrorDialog("Registration created but no user id returned. Try signing in.")
                                    btnNext.isEnabled = true
                                }
                            }
                            .addOnFailureListener { createEx ->
                                if (createEx is FirebaseAuthUserCollisionException) {
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {
                                            val user = auth.currentUser
                                            val uid = user?.uid
                                            if (uid != null) {
                                                firebaseModel.createOrUpdateUserDocument(uid, username, email) { success, err ->
                                                    if (success) {
                                                        saveCredentialsAndContinue(username, email, uid)
                                                    } else {
                                                        if (err != null) showExceptionDialog(err) else showErrorDialog("Failed to write user document after sign-in. Please check Firestore rules.")
                                                        btnNext.isEnabled = true
                                                    }
                                                }
                                            } else {
                                                saveCredentialsAndContinue(username, email, uid)
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
                    }
                    .addOnFailureListener { e ->
                        showErrorDialog("Failed checking username availability: ${e.localizedMessage}")
                        btnNext.isEnabled = true
                    }

            } else {
                db.collection("users").whereEqualTo("name", username).get()
                    .addOnSuccessListener { snap ->
                        if (snap.isEmpty) {
                            Toast.makeText(this, "No user found with that username", Toast.LENGTH_LONG).show()
                            btnNext.isEnabled = true
                            return@addOnSuccessListener
                        }

                        val doc = snap.documents.first()
                        val storedEmail = doc.getString("email")

                        val enteredPassword = password

                        if (storedEmail.isNullOrEmpty()) {
                            Toast.makeText(this, "This account cannot sign in by username only. Please register with email.", Toast.LENGTH_LONG).show()
                            btnNext.isEnabled = true
                            return@addOnSuccessListener
                        }

                        if (enteredPassword.isEmpty()) {
                            Toast.makeText(this, "Please enter your password to sign in", Toast.LENGTH_LONG).show()
                            btnNext.isEnabled = true
                            return@addOnSuccessListener
                        }

                        auth.signInWithEmailAndPassword(storedEmail, enteredPassword)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                val uid = user?.uid ?: doc.getString("userId")
                                if (uid != null) {
                                    firebaseModel.createOrUpdateUserDocument(uid, username, storedEmail) { success, err ->
                                        if (success) {
                                            saveCredentialsAndContinue(username, storedEmail, uid)
                                        } else {
                                            if (err != null) showExceptionDialog(err) else showErrorDialog("Failed to write user document after sign-in. Please check Firestore rules.")
                                            btnNext.isEnabled = true
                                        }
                                    }
                                } else {
                                    saveCredentialsAndContinue(username, storedEmail, uid)
                                }
                            }
                            .addOnFailureListener { signInEx ->
                                showExceptionDialog(signInEx)
                                btnNext.isEnabled = true
                            }
                    }
                    .addOnFailureListener { e ->
                        showExceptionDialog(e)
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

    private fun saveCredentialsAndContinue(username: String, email: String, uid: String?) {
        try {
            val prefs = getSharedPreferences("auth", MODE_PRIVATE)
            prefs.edit {
                putString("name", username)
                putString("email", email)
                putString("uid", uid)
            }
        } catch (_: Exception) {
        }

        showStyledToast("Welcome, $username!", android.R.drawable.ic_menu_send)
        navigateToMainActivity()
    }

    // Styled toast helper
    private fun showStyledToast(message: String, @DrawableRes iconRes: Int? = null) {
        try {
            val inflater = LayoutInflater.from(this)
            val parent = findViewById<ViewGroup>(android.R.id.content)
            val layout = inflater.inflate(R.layout.custom_toast, parent, false)
            val tv = layout.findViewById<TextView>(R.id.toast_text)
            val iv = layout.findViewById<ImageView>(R.id.toast_icon)
            tv.text = message
            if (iconRes != null) {
                iv.setImageResource(iconRes)
                iv.visibility = View.VISIBLE
            } else {
                iv.visibility = View.GONE
            }

            val toast = Toast(this)
            toast.duration = if (iconRes == android.R.drawable.ic_menu_send) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            try {
                val anim = AnimationUtils.loadAnimation(this, R.anim.toast_fade)
                layout.startAnimation(anim)
            } catch (_: Exception) {
            }
            toast.view = layout
            toast.setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 200)
            toast.show()
        } catch (_: Exception) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
