package com.sandg.tastebuds.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class RegistrationViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun isAlreadySignedIn(): Boolean = repository.isSignedIn()

    fun register(username: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val name = repository.register(username, email, password)
                _authState.postValue(AuthState.Success(name))
            } catch (e: Exception) {
                _authState.postValue(AuthState.Error(e.localizedMessage ?: "Registration failed"))
            }
        }
    }

    fun signIn(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val name = repository.signIn(email, password)
                _authState.postValue(AuthState.Success(name))
            } catch (e: Exception) {
                _authState.postValue(AuthState.Error(e.localizedMessage ?: "Sign-in failed"))
            }
        }
    }
}

