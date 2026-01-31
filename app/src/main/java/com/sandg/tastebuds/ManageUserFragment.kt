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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class ManageUserFragment : Fragment() {

    private var cameraImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleAvatarSelected(it) }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && cameraImageUri != null) {
            handleAvatarSelected(cameraImageUri!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_manage_user, container, false)

        val user = FirebaseAuth.getInstance().currentUser
        val ivAvatar = root.findViewById<ImageView>(R.id.ivAvatar)
        val tvName = root.findViewById<TextView>(R.id.tvName)
        val tvEmail = root.findViewById<TextView>(R.id.tvEmail)
        val btnEditAvatar = root.findViewById<Button>(R.id.btnEditAvatar)
        val btnChangePassword = root.findViewById<Button>(R.id.btnChangePassword)
        val btnSignOut = root.findViewById<Button>(R.id.btnSignOut)

        tvName.text = user?.displayName ?: ""
        tvEmail.text = user?.email ?: ""

        // Load avatar from local prefs (internal storage) if present; otherwise fall back to Firebase profile photo
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val avatarKey = "avatar_path_${user?.uid ?: "anonymous"}"
        val localPath = prefs.getString(avatarKey, null)
        if (localPath != null) {
            val f = File(localPath)
            if (f.exists()) {
                try {
                    com.squareup.picasso.Picasso.get().load(f).placeholder(R.drawable.ic_baseline_person_24).into(ivAvatar)
                } catch (_: Exception) {
                    ivAvatar.setImageURI(Uri.fromFile(f))
                }
            }
        } else {
            // fallback to Firebase user photoURL (read-only)
            user?.photoUrl?.let { uri ->
                try {
                    com.squareup.picasso.Picasso.get().load(uri).placeholder(R.drawable.ic_baseline_person_24).into(ivAvatar)
                } catch (_: Exception) {
                    ivAvatar.setImageURI(uri)
                }
            }
        }

        btnEditAvatar.setOnClickListener { showAvatarOptions() }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnSignOut.setOnClickListener {
            performSignOut()
        }

        return root
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

        // Optionally clear user_prefs (avatar path) if you want to remove local avatar on sign out
        // val userPrefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        // userPrefs.edit().clear().apply()

        // Navigate back to RegistrationActivity and clear back stack so user cannot go back
        val intent = Intent(requireContext(), RegistrationActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        activity?.finish()
    }

    private fun showAvatarOptions() {
        val items = arrayOf("Take Photo", "Choose from Files")
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Avatar")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickImageFromFiles()
                }
            }
            .show()
    }

    private fun pickImageFromFiles() {
        pickImageLauncher.launch("image/*")
    }

    private fun launchCamera() {
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        cameraImageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        takePhotoLauncher.launch(cameraImageUri)
    }

    // New: handle selected avatar and save locally (internal storage) instead of uploading to Firebase
    private fun handleAvatarSelected(uri: Uri) {
        showStyledToast("Saving avatar locally...", null)
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            showStyledToast("Not signed in", android.R.drawable.ic_dialog_alert)
            return
        }

        try {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            val filename = "avatar_${user.uid}.jpg"
            val saved = saveBitmapToInternalFile(bitmap, filename)
            if (saved != null) {
                // persist path in SharedPreferences
                val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("avatar_path_${user.uid}", saved.absolutePath).apply()

                // update UI avatar ImageView
                val iv = view?.findViewById<ImageView>(R.id.ivAvatar)
                if (iv != null) {
                    try {
                        com.squareup.picasso.Picasso.get().load(saved).placeholder(R.drawable.ic_baseline_person_24).into(iv)
                    } catch (_: Exception) {
                        iv.setImageURI(Uri.fromFile(saved))
                    }
                }

                showStyledToast("Avatar saved locally", android.R.drawable.ic_menu_send)
            } else {
                showStyledToast("Failed to save avatar", android.R.drawable.ic_dialog_alert)
            }
        } catch (e: Exception) {
            // fallback: try to copy stream from uri to internal file
            try {
                val input = requireContext().contentResolver.openInputStream(uri)
                input?.let {
                    val filename = "avatar_${user.uid}_${UUID.randomUUID()}.jpg"
                    val file = File(requireContext().filesDir, filename)
                    var out: OutputStream? = null
                    try {
                        out = FileOutputStream(file)
                        input.copyTo(out)
                        out.flush()
                        // persist path
                        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        prefs.edit().putString("avatar_path_${user.uid}", file.absolutePath).apply()
                        val iv = view?.findViewById<ImageView>(R.id.ivAvatar)
                        if (iv != null) {
                            try {
                                com.squareup.picasso.Picasso.get().load(file).placeholder(R.drawable.ic_baseline_person_24).into(iv)
                            } catch (_: Exception) {
                                iv.setImageURI(Uri.fromFile(file))
                            }
                        }
                        showStyledToast("Avatar saved locally", android.R.drawable.ic_menu_send)
                    } finally {
                        out?.close()
                        input.close()
                    }
                } ?: run {
                    showStyledToast("Failed to read selected image", android.R.drawable.ic_dialog_alert)
                }
            } catch (ex: IOException) {
                showStyledToast("Failed to save avatar", android.R.drawable.ic_dialog_alert)
            }
        }
    }

    private fun saveBitmapToInternalFile(bitmap: android.graphics.Bitmap, filename: String): File? {
        return try {
            val file = File(requireContext().filesDir, filename)
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                fos.flush()
            } finally {
                fos?.close()
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    // Personalized styled toast (adapted from RegistrationActivity.showStyledToast)
    private fun showStyledToast(message: String, iconRes: Int?) {
        try {
            val inflater = LayoutInflater.from(requireContext())
            val parent = requireActivity().findViewById<ViewGroup>(android.R.id.content)
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

            val toast = Toast(requireContext())
            toast.duration = if (iconRes == android.R.drawable.ic_menu_send) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            try {
                val anim = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.toast_fade)
                layout.startAnimation(anim)
            } catch (_: Exception) {
            }
            toast.view = layout
            toast.setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 200)
            toast.show()
        } catch (_: Exception) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
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
                    showStyledToast(msg ?: "Password changed", android.R.drawable.ic_menu_send)
                    dialog.dismiss()
                } else {
                    // If the failure is due to wrong old password, show the exact message
                    showStyledToast(msg ?: "Failed to change password", android.R.drawable.ic_dialog_alert)
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
                // reauthentication failed — most likely old password incorrect
                onComplete(false, "Old password not correct - could not update password")
            }
        }
    }
}
