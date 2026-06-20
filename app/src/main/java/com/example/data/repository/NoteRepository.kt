package com.example.data.repository

import com.example.data.local.entity.Note
import com.example.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getActiveNotes(): Flow<List<Note>>
    fun getArchivedNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Int): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    fun getNotesByCategory(category: String): Flow<List<Note>>
    suspend fun insertNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun updatePinStatus(id: Int, isPinned: Boolean)
    suspend fun updateArchiveStatus(id: Int, isArchived: Boolean)

    // Category operations
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun editCategoryAndMigration(oldCategory: String, newCategory: String)
    suspend fun deleteCategoryAndMigration(category: Category)
    suspend fun getNotesWithEnabledReminders(): List<Note>

    // Reminder history operations
    fun getAllReminderHistory(): Flow<com.example.data.local.entity.ReminderHistoryEntry> = throw NotImplementedError() // will be customized in implementation
    fun getReminderHistoryFlow(): Flow<List<com.example.data.local.entity.ReminderHistoryEntry>>
    suspend fun insertReminderHistory(entry: com.example.data.local.entity.ReminderHistoryEntry)
    suspend fun clearReminderHistory()
}
