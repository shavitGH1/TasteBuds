package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class RegistrationActivity : AppCompatActivity() {

    private val TAG = "RegistrationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar
        supportActionBar?.hide()

        setContentView(R.layout.activity_registration)

        // Always show login page - no auto-login check
        Log.d(TAG, "Showing registration/login form")

        val etUsername = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnNext = findViewById<Button>(R.id.btnGoToRecipes)

        btnNext.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            Log.d(TAG, "Next button clicked with username: $username")

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Accept any username and password - navigate to recipes
            Toast.makeText(this, "Welcome, $username!", Toast.LENGTH_SHORT).show()
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finish this activity so user can't go back to registration
    }
}

