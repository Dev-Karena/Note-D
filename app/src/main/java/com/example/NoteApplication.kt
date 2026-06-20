package com.example

import android.app.Application
import com.example.data.local.database.AppDatabase
import com.example.data.repository.NoteRepository
import com.example.data.repository.NoteRepositoryImpl

class NoteApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val noteRepository: NoteRepository by lazy { NoteRepositoryImpl(database.noteDao(), database.categoryDao(), database.reminderHistoryDao()) }

    override fun onCreate() {
        super.onCreate()
        com.example.reminder.NotificationHelper.createNotificationChannel(this)
    }
}
