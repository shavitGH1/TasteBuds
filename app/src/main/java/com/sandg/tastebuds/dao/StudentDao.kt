package com.sandg.tastebuds.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sandg.tastebuds.models.Student

@Dao
interface StudentDao {

    @Query("SELECT * FROM Student")
    fun getAllStudents(): List<Student>

    @Query("SELECT * FROM Student WHERE id = :id")
    fun getStudentById(id: String): Student

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStudents(vararg students: Student)

    @Delete
    fun deleteStudent(student: Student)
}