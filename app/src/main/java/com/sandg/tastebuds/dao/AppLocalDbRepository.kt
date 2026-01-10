package com.sandg.tastebuds.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sandg.tastebuds.models.Student

@Database(entities = [Student::class], version = 2)
abstract class AppLocalDbRepository: RoomDatabase() {
    abstract val studentDao: StudentDao
}