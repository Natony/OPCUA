// app/src/main/java/com/example/s7opcuaapp/data/local/DatabaseMigrations.kt
package com.example.s7opcuaapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add flag for password reset requirement
            database.execSQL(
                "ALTER TABLE users ADD COLUMN requirePasswordChange INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    // Add more migrations as needed
    val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
}