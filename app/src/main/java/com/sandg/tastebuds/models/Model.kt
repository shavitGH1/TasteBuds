package com.sandg.tastebuds.models

import android.os.Handler
import android.os.Looper
import com.sandg.tastebuds.base.Completion
import com.sandg.tastebuds.base.StudentCompletion
import com.sandg.tastebuds.base.StudentsCompletion
import com.sandg.tastebuds.dao.AppLocalDB
import com.sandg.tastebuds.dao.AppLocalDbRepository
import java.util.concurrent.Executors

class Model private constructor() {

    private val firebaseModel = FirebaseModel()
    private val firebaseAuth = FirebaseAuthModel()

//    private val executor = Executors.newSingleThreadExecutor()
//    private val mainHandler = Handler.createAsync(Looper.getMainLooper())
//
//    private val database: AppLocalDbRepository = AppLocalDB.db

    companion object {
        val shared = Model()
    }

    fun getAllStudents(completion: StudentsCompletion) {


        firebaseAuth.signIn("tal.ziii@colman.ac.il", "123456") {

        }

        firebaseModel.getAllStudents(completion)
//
//        executor.execute {
//            val students = database.studentDao.getAllStudents()
//            mainHandler.post {
//                completion(students)
//            }
//        }
    }

    fun addStudent(student: Student, completion: Completion) {
        firebaseModel.addStudent(student, completion)

//        executor.execute {
//            database.studentDao.insertStudents(student)
//            mainHandler.post {
//                completion()
//            }
//        }
    }

    fun deleteStudent(student: Student) {
        firebaseModel.deleteStudent(student)
//        database.studentDao.deleteStudent(student)
    }

    fun getStudentById(id: String, completion: StudentCompletion) {
        firebaseModel.getStudentById(id, completion)

//        executor.execute {
//            val student = database.studentDao.getStudentById(id)
//            mainHandler.post {
//                completion(student)
//            }
//        }
    }
}

