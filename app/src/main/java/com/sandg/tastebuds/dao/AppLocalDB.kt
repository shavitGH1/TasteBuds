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

            // Ensure Recipe table has the newer columns added in later schema versions.
            // Some existing installs may have an older Recipe table (missing publisherId).
            // Check pragma table_info for 'Recipe' and add missing columns via ALTER TABLE.
            var cursor: android.database.Cursor? = null
            try {
                cursor = database.query("PRAGMA table_info(`Recipe`)")
                var hasPublisherId = false
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val colNameIndex = cursor.getColumnIndex("name")
                        if (colNameIndex >= 0) {
                            val colName = cursor.getString(colNameIndex)
                            if (colName == "publisherId") {
                                hasPublisherId = true
                                break
                            }
                        }
                    }
                }
                if (!hasPublisherId) {
                    // add publisherId as nullable TEXT (matches entity declaration)
                    database.execSQL("ALTER TABLE `Recipe` ADD COLUMN `publisherId` TEXT")
                }
            } catch (e: Exception) {
                // Best-effort: log via System (avoid adding Android Log dependency here)
                try { System.err.println("AppLocalDB MIGRATION_4_5: schema check failed: " + e) } catch (_: Exception) {}
            } finally {
                try { cursor?.close() } catch (_: Exception) {}
            }
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