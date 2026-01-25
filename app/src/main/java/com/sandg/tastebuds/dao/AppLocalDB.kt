package com.sandg.tastebuds.dao

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sandg.tastebuds.MyApplication

object AppLocalDB {

    // Migration from version 4 -> 5: create Favorite table (recipeId, userId) without destructive migration
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create Favorite table with composite primary key (recipeId, userId)
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `Favorite` (
                    `recipeId` TEXT NOT NULL,
                    `userId` TEXT NOT NULL,
                    `isFavorite` INTEGER NOT NULL,
                    PRIMARY KEY(`recipeId`, `userId`)
                )
            """.trimIndent())
        }
    }

    val db: AppLocalDbRepository by lazy {

        val context = MyApplication.appContext
            ?: throw IllegalStateException("Context is null")

        Room.databaseBuilder(
            context = context,
            klass = AppLocalDbRepository::class.java,
            name = "recipes.db"
        )
            .addMigrations(MIGRATION_4_5)
            .build()
    }
}