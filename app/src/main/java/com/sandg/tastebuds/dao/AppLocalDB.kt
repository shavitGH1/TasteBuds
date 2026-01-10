package com.sandg.tastebuds.dao

import androidx.room.Room
import com.sandg.tastebuds.MyApplication

object AppLocalDB {

    val db: AppLocalDbRepository by lazy {

        val context = MyApplication.appContext
            ?: throw IllegalStateException("Context is null")

        Room.databaseBuilder(
            context = context,
            klass = AppLocalDbRepository::class.java,
            name = "students.db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
}