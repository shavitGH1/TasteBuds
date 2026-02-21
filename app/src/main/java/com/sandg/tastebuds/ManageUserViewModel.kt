package com.sandg.tastebuds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.sandg.tastebuds.models.FirebaseModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class NameUpdated(val name: String) : ProfileState()
    object PasswordChanged : ProfileState()
    object SignedOut : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ManageUserViewModel : ViewModel() {

    private val firebaseModel = FirebaseModel()

    private val _state = MutableLiveData<ProfileState>(ProfileState.Idle)
    val state: LiveData<ProfileState> = _state

    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String> = _displayName

    fun loadUserName(cachedName: String?) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!cachedName.isNullOrBlank()) { _displayName.value = cachedName; return }
        viewModelScope.launch {
            val doc = runCatching { firebaseModel.getUserByIdSync(user.uid) }.getOrNull()
            val name = (doc?.get("name") as? String)?.takeIf { it.isNotBlank() }
            _displayName.postValue(name ?: user.displayName ?: "")
        }
    }

    fun updateName(newName: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        _state.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                firebaseModel.createOrUpdateUserDocumentSync(user.uid, newName, user.email ?: "")
                val request = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                user.updateProfile(request).await()
                _displayName.postValue(newName)
                _state.postValue(ProfileState.NameUpdated(newName))
            } catch (e: Exception) {
                _state.postValue(ProfileState.Error(e.localizedMessage ?: "Failed to update name"))
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email ?: return
        _state.value = ProfileState.Loading
        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
                user.reauthenticate(credential).await()
                user.updatePassword(newPassword).await()
                _state.postValue(ProfileState.PasswordChanged)
            } catch (e: Exception) {
                _state.postValue(ProfileState.Error(e.localizedMessage ?: "Failed to change password"))
            }
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        _state.value = ProfileState.SignedOut
    }

    fun resetState() { _state.value = ProfileState.Idle }
}

