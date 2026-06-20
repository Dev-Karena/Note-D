package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // E.g., "Work", "Personal", "Study", "Ideas"
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val reminderTime: Long? = null,
    val color: Int, // Color hex Int
    val isArchived: Boolean = false,
    val repeatType: RepeatType = RepeatType.NONE,
    val isReminderEnabled: Boolean = false
)
