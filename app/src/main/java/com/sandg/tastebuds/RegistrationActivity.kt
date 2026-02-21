package com.sandg.tastebuds

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.sandg.tastebuds.auth.AuthState
import com.sandg.tastebuds.auth.RegistrationViewModel
import com.sandg.tastebuds.databinding.ActivityRegistrationBinding

class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private val viewModel: RegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // If already signed in, skip to main screen immediately
        if (viewModel.isAlreadySignedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupModeSwitch()
        setupSubmitButton()
        observeAuthState()
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupModeSwitch() {
        updateFieldsForMode(binding.switchMode.isChecked)
        binding.switchMode.setOnCheckedChangeListener { _, isChecked ->
            updateFieldsForMode(isChecked)
        }
    }

    private fun updateFieldsForMode(isRegister: Boolean) {
        binding.etUsername.visibility = if (isRegister) View.VISIBLE else View.GONE
        binding.btnGoToRecipes.text =
            if (isRegister) getString(R.string.button_register)
            else getString(R.string.button_sign_in_username)
    }

    private fun setupSubmitButton() {
        binding.btnGoToRecipes.setOnClickListener {
            if (!validateInput()) return@setOnClickListener
            submit()
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validateInput(): Boolean {
        val isRegister = binding.switchMode.isChecked
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (isRegister && username.isEmpty()) {
            showStyledToast("Please enter a username"); return false
        }
        if (email.isEmpty() || password.isEmpty()) {
            showStyledToast("Please enter email and password"); return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showStyledToast("Please enter a valid email address"); return false
        }
        if (password.length < 6) {
            showStyledToast("Password must be at least 6 characters"); return false
        }
        return true
    }

    // ── ViewModel call ────────────────────────────────────────────────────────

    private fun submit() {
        val isRegister = binding.switchMode.isChecked
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (isRegister) viewModel.register(username, email, password)
        else viewModel.signIn(email, password)
    }

    // ── Observe ───────────────────────────────────────────────────────────────

    private fun observeAuthState() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> setLoading(true)
                is AuthState.Success -> onAuthSuccess(state.username)
                is AuthState.Error -> {
                    setLoading(false)
                    showStyledToast(state.message, android.R.drawable.ic_dialog_alert)
                }
                else -> setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnGoToRecipes.isEnabled = !loading
    }

    private fun onAuthSuccess(username: String) {
        // Save only non-sensitive data (no password)
        getSharedPreferences("auth", MODE_PRIVATE).edit {
            putString("name", username)
        }
        showStyledToast("Welcome, $username!", android.R.drawable.ic_menu_send, true)
        navigateToMain()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
