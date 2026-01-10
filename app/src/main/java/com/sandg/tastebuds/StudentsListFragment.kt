package com.sandg.tastebuds

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.sandg.tastebuds.databinding.FragmentStudentsListBinding
import com.sandg.tastebuds.models.Model
import com.sandg.tastebuds.models.Student

class StudentsListFragment : Fragment() {

    private var binding: FragmentStudentsListBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStudentsListBinding.inflate(layoutInflater, container, false)
        setupRecyclerView()
        return binding?.root
    }

    private fun setupRecyclerView() {
        val layout = LinearLayoutManager(context)
        binding?.recyclerView?.layoutManager = layout
        binding?.recyclerView?.setHasFixedSize(true)

        binding?.progressBar?.visibility = View.VISIBLE
        Model.shared.getAllStudents { students ->

            binding?.progressBar?.visibility = View.GONE
            val adapter = StudentsAdapter(students)
            adapter.listener = object : OnItemClickListener {

                override fun onStudentItemClick(student: Student) {
                    navigateToPinkFragment(student)
                }
            }
            binding?.recyclerView?.adapter = adapter
        }

    }

    private fun navigateToPinkFragment(student: Student) {
        view?.let {
            val action = StudentsListFragmentDirections.actionStudentsListFragmentToBlueFragment(student.name)
            Navigation.findNavController(it).navigate(action)
        }
    }
}