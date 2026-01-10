package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.sandg.tastebuds.databinding.FragmentAddStudentBinding
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Student
import kotlin.text.clear

class AddStudentFragment : Fragment() {

    private var binding: FragmentAddStudentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddStudentBinding.inflate(layoutInflater, container, false)
        setupView()
        setHasOptionsMenu(true)
        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }

    private fun setupView() {

        binding?.loadingIndicator?.visibility = View.GONE

        binding?.cancelButton?.setOnClickListener {
            dismiss()
        }

        binding?.saveStudentButton?.setOnClickListener {

            binding?.loadingIndicator?.visibility = View.VISIBLE

            val studentName: String = binding?.nameEditText?.text.toString()
            val studentId: String = binding?.idEditText?.text.toString()

            val student = Student(
                name = studentName,
                id = studentId,
                isPresent = false,
                avatarUrlString = null
            )

            Model.shared.addStudent(student) {
                dismiss()
            }
        }
    }


    private fun dismiss() {
        view?.findNavController()?.popBackStack()
    }
}