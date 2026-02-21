package com.sandg.tastebuds

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.sandg.tastebuds.databinding.FragmentManageUserBinding
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream

class ManageUserFragment : Fragment() {

    private var binding: FragmentManageUserBinding? = null
    private val viewModel: ManageUserViewModel by viewModels()
    private var cameraImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handlePhotoSelected(it) }
    }
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraImageUri?.let { handlePhotoSelected(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentManageUserBinding.inflate(inflater, container, false)
        val user = FirebaseAuth.getInstance().currentUser

        val savedName = requireContext()
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("name", null)

        viewModel.loadUserName(savedName)
        loadProfilePhoto(user?.uid)

        binding?.tvEmail?.text = user?.email ?: ""

        observeViewModel()
        setupButtons()

        return binding?.root
    }

    // ── Observers ─────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.displayName.observe(viewLifecycleOwner) { name ->
            binding?.tvName?.text = name.ifBlank { "Tap ✏ to set your name" }
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileState.NameUpdated -> {
                    requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .edit().putString("name", state.name).apply()
                    showStyledToast("Name updated!", android.R.drawable.ic_menu_save, true)
                    viewModel.resetState()
                }
                is ProfileState.PasswordChanged -> {
                    showStyledToast("Password changed!", android.R.drawable.ic_lock_lock, true)
                    viewModel.resetState()
                }
                is ProfileState.SignedOut -> navigateToRegistration()
                is ProfileState.Error -> {
                    showStyledToast(state.message)
                    viewModel.resetState()
                }
                else -> Unit
            }
        }
    }

    // ── Button setup ──────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding?.btnEditPhoto?.setOnClickListener { showPhotoOptions() }
        binding?.btnEditName?.setOnClickListener { showEditNameDialog() }
        binding?.btnChangePassword?.setOnClickListener { showChangePasswordDialog() }
        binding?.btnSignOut?.setOnClickListener { viewModel.signOut() }
    }

    // ── Profile photo ─────────────────────────────────────────────────────────

    private fun loadProfilePhoto(uid: String?) {
        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("photo_path_${uid ?: "anonymous"}", null)
        val file = path?.let { File(it) }?.takeIf { it.exists() }

        if (file != null) {
            Picasso.get().load(file).resize(120, 120).centerCrop().into(binding?.ivAvatar)
            binding?.ivAvatar?.imageTintList = null
        } else {
            binding?.ivAvatar?.setImageResource(R.drawable.ic_baseline_person_24)
        }
    }

    private fun showPhotoOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> pickImageLauncher.launch("image/*")
                    2 -> removePhoto()
                }
            }.show()
    }

    private fun launchCamera() {
        val values = ContentValues().apply { put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg") }
        cameraImageUri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        takePhotoLauncher.launch(cameraImageUri)
    }

    private fun removePhoto() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit { remove("photo_path_${uid ?: "anonymous"}") }
        binding?.ivAvatar?.setImageResource(R.drawable.ic_baseline_person_24)
        showStyledToast("Photo removed")
    }

    private fun handlePhotoSelected(uri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        try {
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().contentResolver, uri))
            val file = File(requireContext().filesDir, "profile_photo_${user.uid}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }

            requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit { putString("photo_path_${user.uid}", file.absolutePath) }

            binding?.ivAvatar?.let { iv ->
                Picasso.get().invalidate(file)
                Picasso.get().load(file)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .resize(120, 120).centerCrop().into(iv)
                iv.imageTintList = null
            }
            showStyledToast("Photo saved!", android.R.drawable.ic_menu_camera, true)
        } catch (_: Exception) {
            showStyledToast("Failed to save photo")
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showEditNameDialog() {
        val current = viewModel.displayName.value ?: ""
        val input = EditText(requireContext()).apply { setText(current); hint = "Your name"; setSingleLine(); setPadding(48, 32, 48, 32) }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Name").setView(input)
            .setPositiveButton("Save", null).setNegativeButton("Cancel", null)
            .create().also { dialog ->
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val name = input.text.toString().trim()
                    if (name.isEmpty()) { input.error = "Name cannot be empty"; return@setOnClickListener }
                    viewModel.updateName(name)
                    dialog.dismiss()
                }
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNew = dialogView.findViewById<EditText>(R.id.etNewPassword)

        AlertDialog.Builder(requireContext())
            .setView(dialogView).setPositiveButton("Change", null).setNegativeButton("Cancel", null)
            .create().also { dialog ->
                dialog.show()
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val old = etOld.text.toString().trim()
                    val new = etNew.text.toString().trim()
                    if (old.isEmpty()) { etOld.error = "Required"; return@setOnClickListener }
                    if (new.length < 6) { etNew.error = "Minimum 6 characters"; return@setOnClickListener }
                    viewModel.changePassword(old, new)
                    dialog.dismiss()
                }
            }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun navigateToRegistration() {
        requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE).edit { clear() }
        val intent = Intent(requireContext(), RegistrationActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
