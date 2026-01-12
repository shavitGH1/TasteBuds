package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide the action bar
        supportActionBar?.hide()

        setContentView(R.layout.activity_registration)

        val btnGoToRecipes = findViewById<Button>(R.id.btnGoToRecipes)
        btnGoToRecipes.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}

