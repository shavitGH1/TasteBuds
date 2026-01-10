package com.sandg.tastebuds.models

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.StudentCompletion
import com.sandg.tastebuds.base.StudentsCompletion

class FirebaseModel {

    private val db = Firebase.firestore

    private companion object COLLECTIONS {
        const val STUDENTS = "students"
    }

    fun getAllStudents(completion: StudentsCompletion) {
        db.collection(STUDENTS).get()
            .addOnCompleteListener { result ->
                when (result.isSuccessful) {
                    true -> completion(result.result.map { Student.fromJson(it.data) })
                    false -> completion(emptyList())
                }
            }
    }

    fun addStudent(student: Student, completion: Completion) {
        db.collection(STUDENTS)
            .document(student.id)
            .set(student.toJson)
            .addOnSuccessListener { documentReference ->
                completion()
            }
            .addOnFailureListener { e ->
                completion()
            }

    }

    fun deleteStudent(student: Student) {

    }

    fun getStudentById(id: String, completion: StudentCompletion) {

    }
}
