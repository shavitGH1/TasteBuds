package com.sandg.tastebuds

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream

class ManageUserFragment : Fragment() {

    private var cameraImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePhotoSelected(it) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && cameraImageUri != null) {
            handlePhotoSelected(cameraImageUri!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_manage_user, container, false)

        val user = FirebaseAuth.getInstance().currentUser
        val ivAvatar = root.findViewById<ImageView>(R.id.ivAvatar)
        val tvName = root.findViewById<TextView>(R.id.tvName)
        val tvEmail = root.findViewById<TextView>(R.id.tvEmail)
        val tvRecipeCount = root.findViewById<TextView>(R.id.tvRecipeCount)
        val btnEditPhoto = root.findViewById<Button>(R.id.btnEditPhoto)
        val btnChangePassword = root.findViewById<Button>(R.id.btnChangePassword)
        val btnSignOut = root.findViewById<Button>(R.id.btnSignOut)

        // Set user info
        val displayName = user?.displayName ?: user?.email?.substringBefore("@") ?: "User"
        tvName.text = displayName
        tvEmail.text = user?.email ?: ""

        // Load saved photo or show default icon
        loadProfilePhoto(ivAvatar, user?.uid)

        // Load stats using SharedRecipesViewModel
        try {
            val sharedVm = androidx.lifecycle.ViewModelProvider(requireActivity())[SharedRecipesViewModel::class.java]
            sharedVm.recipes.observe(viewLifecycleOwner) { recipes ->
                val currentUid = user?.uid
                val myRecipesCount = recipes.count { it.publisherId == currentUid }
                tvRecipeCount.text = myRecipesCount.toString()
            }
        } catch (e: Exception) {
            tvRecipeCount.text = "0"
        }

        btnEditPhoto.setOnClickListener { showPhotoOptions() }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnSignOut.setOnClickListener {
            performSignOut()
        }

        return root
    }

    private fun loadProfilePhoto(ivAvatar: ImageView, uid: String?) {
        // Try to load saved photo from internal storage
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val photoPath = prefs.getString("photo_path_${uid ?: "anonymous"}", null)

        if (photoPath != null) {
            val file = File(photoPath)
            if (file.exists()) {
                try {
                    com.squareup.picasso.Picasso.get()
                        .load(file)
                        .resize(120, 120)
                        .centerCrop()
                        .into(ivAvatar)
                    // Remove tint when showing photo
                    ivAvatar.imageTintList = null
                } catch (e: Exception) {
                    // Fallback to default icon
                    ivAvatar.setImageResource(R.drawable.ic_baseline_person_24)
                }
            } else {
                // File doesn't exist, show default
                ivAvatar.setImageResource(R.drawable.ic_baseline_person_24)
            }
        } else {
            // No saved photo, show default icon
            ivAvatar.setImageResource(R.drawable.ic_baseline_person_24)
        }
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickFromGallery()
                    2 -> removePhoto()
                }
            }
            .show()
    }

    private fun launchCamera() {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        cameraImageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        takePhotoLauncher.launch(cameraImageUri)
    }

    private fun pickFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun removePhoto() {
        val user = FirebaseAuth.getInstance().currentUser
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("photo_path_${user?.uid ?: "anonymous"}").apply()

        // Reset to default icon
        val ivAvatar = view?.findViewById<ImageView>(R.id.ivAvatar)
        ivAvatar?.setImageResource(R.drawable.ic_baseline_person_24)

        showStyledToast("Photo removed")
    }

    private fun handlePhotoSelected(uri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            showStyledToast("Not signed in")
            return
        }

        try {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)

            // Save to internal storage
            val filename = "profile_photo_${user.uid}.jpg"
            val file = File(requireContext().filesDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Save path in preferences
            val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("photo_path_${user.uid}", file.absolutePath).apply()

            // Update UI
            val ivAvatar = view?.findViewById<ImageView>(R.id.ivAvatar)
            ivAvatar?.let {
                com.squareup.picasso.Picasso.get()
                    .load(file)
                    .resize(120, 120)
                    .centerCrop()
                    .into(it)
                // Remove tint when showing photo
                it.imageTintList = null
            }

            showStyledToast("Photo saved!", android.R.drawable.ic_menu_camera, true)
        } catch (e: Exception) {
            showStyledToast("Failed to save photo")
        }
    }

    private fun performSignOut() {
        // Firebase sign out
        FirebaseAuth.getInstance().signOut()

        // Clear locally stored auth credentials
        try {
            val prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {
        }

        // Navigate back to RegistrationActivity and clear back stack so user cannot go back
        val intent = Intent(requireContext(), RegistrationActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        activity?.finish()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNew = dialogView.findViewById<EditText>(R.id.etNewPassword)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Change", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Now override the positive button to validate inputs before dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val old = etOld.text.toString().trim()
            val nw = etNew.text.toString().trim()

            if (old.isEmpty()) {
                etOld.error = "Required"
                return@setOnClickListener
            }
            if (nw.length < 6) {
                etNew.error = "New password must be at least 6 characters"
                return@setOnClickListener
            }

            // Disable button to prevent double-click
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            changePassword(old, nw) { success, msg ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                if (success) {
                    showStyledToast(msg ?: "Password changed", android.R.drawable.ic_lock_lock, true)
                    dialog.dismiss()
                } else {
                    showStyledToast(msg ?: "Failed to change password")
                }
            }
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String, onComplete: (Boolean, String?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onComplete(false, "Not signed in")
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            onComplete(false, "No email to reauthenticate")
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        onComplete(true, "Password changed successfully")
                    } else {
                        onComplete(false, "Failed to update password")
                    }
                }
            } else {
                // reauthentication failed  most likely old password incorrect
                onComplete(false, "Old password not correct - could not update password")
            }
        }
    }
}
