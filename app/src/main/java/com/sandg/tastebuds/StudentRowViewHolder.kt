package com.sandg.tastebuds

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.sandg.tastebuds.databinding.StudentRowLayoutBinding
import com.sandg.tastebuds.models.Student
import com.squareup.picasso.Picasso

class StudentRowViewHolder(
    private val binding: StudentRowLayoutBinding,
    private val listener: OnItemClickListener?
): RecyclerView.ViewHolder(binding.root) {

    private var student: Student? = null

    init {
        binding.checkbox.setOnClickListener { view ->
            (view?.tag as? Int)?.let { tag ->
                student?.isPresent = binding.checkbox.isChecked
            }
        }

        itemView.setOnClickListener {
            student?.let { student ->
                listener?.onStudentItemClick(student)
            }
        }
    }

    fun bind(student: Student, position: Int) {
        this.student = student
        binding.nameTextView.text = student.name
        binding.idTextView.text = student.id
        binding.checkbox.apply {
            isChecked = student.isPresent
            tag = position
        }
        Log.v("TAG", "Loading image from URL: ${student.avatarUrlString}")
        Picasso
            .get()
            .load(student.avatarUrlString)
            .into(binding.imageView)
    }
}