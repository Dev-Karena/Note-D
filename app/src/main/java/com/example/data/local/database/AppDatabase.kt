package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ReminderHistoryDao
import com.example.data.local.entity.Note
import com.example.data.local.entity.Category
import com.example.data.local.entity.ReminderHistoryEntry

@Database(entities = [Note::class, Category::class, ReminderHistoryEntry::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderHistoryDao(): ReminderHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add repeatType column (stored as String/TEXT, non-null, default 'NONE')
                db.execSQL("ALTER TABLE notes ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'NONE'")
                
                // Add isReminderEnabled column (stored as Integer/Boolean, non-null, default 0/false)
                db.execSQL("ALTER TABLE notes ADD COLUMN isReminderEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminder_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`noteId` INTEGER NOT NULL, " +
                    "`noteTitle` TEXT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "noted_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
