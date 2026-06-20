package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_history")
data class ReminderHistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val noteId: Int,
    val noteTitle: String,
    val timestamp: Long,
    val status: String // "TRIGGERED", "SNOOZED", "DISMISSED"
)
