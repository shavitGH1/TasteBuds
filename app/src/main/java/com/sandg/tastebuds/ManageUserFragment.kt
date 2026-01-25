package com.sandg.tastebuds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class ManageUserFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_manage_user, container, false)

        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val tvName = root.findViewById<TextView>(R.id.tvName)
        val tvEmail = root.findViewById<TextView>(R.id.tvEmail)
        val btnSignOut = root.findViewById<Button>(R.id.btnSignOut)

        tvName.text = user?.displayName ?: ""
        tvEmail.text = user?.email ?: ""

        btnSignOut.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            activity?.recreate()
        }

        return root
    }
}
