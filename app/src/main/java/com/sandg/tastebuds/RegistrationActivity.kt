package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.databinding.ActivityRegistrationBinding
import com.sandg.tastebuds.models.FirebaseModel
import com.sandg.tastebuds.models.Recipe

class RegistrationActivity : AppCompatActivity() {

    private val TAG = "RegistrationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Use view binding to access views
        val binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "Showing registration/login form (with view binding)")

        val switchMode = binding.switchMode
        val etUsername = binding.etUsername
        val etEmail = binding.etEmail
        val etPassword = binding.etPassword
        val btnNext = binding.btnGoToRecipes
        val btnTestFirebase = binding.btnTestFirebase

        val auth = FirebaseAuth.getInstance()
        val db = Firebase.firestore
        val firebaseModel = FirebaseModel()

        // Initialize UI visibility based on mode (checked = Register)
        fun updateFieldsForMode(isRegister: Boolean) {
            // Email only for register; password should be available in both modes so user can enter it when signing-in
            etEmail.visibility = if (isRegister) View.VISIBLE else View.GONE
            etPassword.visibility = View.VISIBLE
            // Update button text to be clearer
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

            // Disable button to avoid double submissions
            btnNext.isEnabled = false

            if (isRegister) {
                // Registration: require email and password
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    btnNext.isEnabled = true
                    return@setOnClickListener
                }

                // Basic validation: email format and minimum password length
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

                // Ensure username isn't already taken
                db.collection("users").whereEqualTo("name", username).get()
                    .addOnSuccessListener { snap ->
                        if (!snap.isEmpty) {
                            Toast.makeText(this, "Username already taken, choose another", Toast.LENGTH_LONG).show()
                            btnNext.isEnabled = true
                            return@addOnSuccessListener
                        }

                        // Create the user with email/password (preferred). If user already exists, try sign-in.
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                val uid = user?.uid

                                if (uid != null) {
                                    // Use FirebaseModel to write the user doc and sample recipes
                                    firebaseModel.createOrUpdateUserDocument(uid, username, email) { success, err ->
                                        if (!success) {
                                            if (err != null) showExceptionDialog(err) else showErrorDialog("Failed to write user document to Firestore. Please check your Firestore rules and network.")
                                            btnNext.isEnabled = true
                                            return@createOrUpdateUserDocument
                                        }
                                        // create sample recipes using model.addRecipe (non-blocking)
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

                                        // now save creds and continue
                                        saveCredentialsAndContinue(username, email, uid)
                                    }
                                } else {
                                    // Unexpected: user created but uid is null
                                    showErrorDialog("Registration created but no user id returned. Try signing in.")
                                    btnNext.isEnabled = true
                                }
                            }
                            .addOnFailureListener { createEx ->
                                // If the email is already in use, attempt sign-in (user might be re-registering)
                                if (createEx is FirebaseAuthUserCollisionException) {
                                    auth.signInWithEmailAndPassword(email, password)
                                        .addOnSuccessListener {
                                            val user = auth.currentUser
                                            val uid = user?.uid
                                            if (uid != null) {
                                                // ensure user doc exists
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
                                            // Show full exception for debugging
                                            showExceptionDialog(signInEx)
                                            Log.e(TAG, "Sign-in after collision failed", signInEx)
                                            btnNext.isEnabled = true
                                        }
                                } else {
                                    // Provide more helpful error messages for common auth failures
                                    // Show full exception for debugging
                                    showExceptionDialog(createEx)
                                    Log.e(TAG, "createUser failed", createEx)
                                    btnNext.isEnabled = true
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed checking username availability", e)
                        showErrorDialog("Failed checking username availability: ${e.localizedMessage}")
                        btnNext.isEnabled = true
                    }

            } else {
                // Sign-in by username (require entered password):
                // Find user doc by username to resolve email; require the user to enter their password to sign in.
                db.collection("users").whereEqualTo("name", username).get()
                    .addOnSuccessListener { snap ->
                        if (snap.isEmpty) {
                            Toast.makeText(this, "No user found with that username", Toast.LENGTH_LONG).show()
                            btnNext.isEnabled = true
                            return@addOnSuccessListener
                        }

                        // Take the first matching document
                        val doc = snap.documents.first()
                        val storedEmail = doc.getString("email")

                        // User must enter their password (we do not store plaintext passwords anymore)
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
                                // ensure user doc exists and has current email/name mapping
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
                                Log.e(TAG, "Sign-in by username failed", signInEx)
                                showExceptionDialog(signInEx)
                                btnNext.isEnabled = true
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed looking up username", e)
                        showExceptionDialog(e)
                        btnNext.isEnabled = true
                    }
            }
        }

        btnTestFirebase.setOnClickListener {
            // First do a low-level connectivity check to www.googleapis.com:443 so we can distinguish
            // network/DNS/connectivity issues from Firestore rules or auth config problems.
            Thread {
                try {
                    val socket = java.net.Socket()
                    val addr = java.net.InetSocketAddress("www.googleapis.com", 443)
                    socket.connect(addr, 3000)
                    socket.close()

                    // If socket connect succeeded, attempt Firestore write on main thread
                    runOnUiThread {
                        val testDoc = mapOf("ts" to System.currentTimeMillis(), "app" to "TasteBuds", "test" to true)
                        val testId = "_test_connection"
                        db.collection("users").document(testId).set(testDoc)
                            .addOnSuccessListener {
                                AlertDialog.Builder(this)
                                    .setTitle("Firebase Test")
                                    .setMessage("Connectivity OK and Firestore write succeeded (users/_test_connection). You are connected to Firebase.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                // Firestore write failed despite connectivity; show full exception
                                showExceptionDialog(e)
                            }
                    }
                } catch (e: Exception) {
                    // Connectivity failed; show exception details on main thread
                    runOnUiThread {
                        showExceptionDialog(e)
                    }
                }
            }.start()
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Registration error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showExceptionDialog(t: Throwable) {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        t.printStackTrace(pw)
        val full = StringBuilder()
        full.append(t.toString()).append("\n\n")
        full.append(sw.toString())

        AlertDialog.Builder(this)
            .setTitle("Registration error (details)")
            .setMessage(full.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveCredentialsAndContinue(username: String, email: String, uid: String?) {
        try {
            val prefs = getSharedPreferences("auth", MODE_PRIVATE)
            prefs.edit {
                putString("name", username)
                putString("email", email)
                putString("uid", uid)
            }
            Log.d(TAG, "Saved credentials to SharedPreferences 'auth'")
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving credentials", e)
        }

        Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_SHORT).show()
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
