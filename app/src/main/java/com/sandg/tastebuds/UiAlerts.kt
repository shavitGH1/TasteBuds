package com.sandg.tastebuds

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import android.view.animation.AnimationUtils

// Reusable styled toast that mirrors RegistrationActivity's showStyledToast
fun Context.showStyledToast(message: String, @DrawableRes iconRes: Int? = null, short: Boolean = false) {
    try {
        val inflater = LayoutInflater.from(this)
        val parent = (this as? android.app.Activity)?.findViewById<ViewGroup>(android.R.id.content)
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

        val toast = Toast(this)
        toast.duration = if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        try {
            val anim = AnimationUtils.loadAnimation(this, R.anim.toast_fade)
            layout.startAnimation(anim)
        } catch (_: Exception) {
        }
        toast.view = layout
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 200)
        toast.show()
    } catch (_: Exception) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

// Fragment convenience
fun Fragment.showStyledToast(message: String, @DrawableRes iconRes: Int? = null, short: Boolean = false) {
    context?.showStyledToast(message, iconRes, short)
}

