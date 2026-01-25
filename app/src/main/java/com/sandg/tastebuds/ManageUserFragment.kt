package com.sandg.tastebuds

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
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
                    com.squareup.picasso.Picasso.get().load(f).placeholder(R.drawable.avatar).into(ivAvatar)
                } catch (_: Exception) {
                    ivAvatar.setImageURI(Uri.fromFile(f))
                }
            }
        } else {
            // fallback to Firebase user photoURL (read-only)
            user?.photoUrl?.let { uri ->
                try {
                    com.squareup.picasso.Picasso.get().load(uri).placeholder(R.drawable.avatar).into(ivAvatar)
                } catch (_: Exception) {
                    ivAvatar.setImageURI(uri)
                }
            }
        }

        btnEditAvatar.setOnClickListener { showAvatarOptions() }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            activity?.recreate()
        }

        return root
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
        Toast.makeText(requireContext(), "Saving avatar locally...", Toast.LENGTH_SHORT).show()
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
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
                        com.squareup.picasso.Picasso.get().load(saved).placeholder(R.drawable.avatar).into(iv)
                    } catch (_: Exception) {
                        iv.setImageURI(Uri.fromFile(saved))
                    }
                }

                Toast.makeText(requireContext(), "Avatar saved locally", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to save avatar", Toast.LENGTH_SHORT).show()
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
                                com.squareup.picasso.Picasso.get().load(file).placeholder(R.drawable.avatar).into(iv)
                            } catch (_: Exception) {
                                iv.setImageURI(Uri.fromFile(file))
                            }
                        }
                        Toast.makeText(requireContext(), "Avatar saved locally", Toast.LENGTH_SHORT).show()
                    } finally {
                        out?.close()
                        input.close()
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "Failed to read selected image", Toast.LENGTH_SHORT).show()
                }
            } catch (ex: IOException) {
                Toast.makeText(requireContext(), "Failed to save avatar", Toast.LENGTH_SHORT).show()
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
            changePassword(old, nw) { success ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                if (success) {
                    Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed to change password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String, onComplete: (Boolean) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
            onComplete(false)
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No email to reauthenticate", Toast.LENGTH_SHORT).show()
            onComplete(false)
            return
        }

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential).addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
            } else {
                onComplete(false)
            }
        }
    }
}
