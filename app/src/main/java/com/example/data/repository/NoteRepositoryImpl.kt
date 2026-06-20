package com.example.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.entity.Note
import com.example.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val reminderHistoryDao: com.example.data.local.dao.ReminderHistoryDao
) : NoteRepository {

    override fun getActiveNotes(): Flow<List<Note>> = noteDao.getActiveNotes()

    override fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    override suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    override fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    override fun getNotesByCategory(category: String): Flow<List<Note>> = noteDao.getNotesByCategory(category)

    override suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    override suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    override suspend fun updatePinStatus(id: Int, isPinned: Boolean) {
        noteDao.updatePinStatus(id, isPinned, System.currentTimeMillis())
    }

    override suspend fun updateArchiveStatus(id: Int, isArchived: Boolean) {
        noteDao.updateArchiveStatus(id, isArchived, System.currentTimeMillis())
    }

    // Category operations
    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    override suspend fun editCategoryAndMigration(oldCategory: String, newCategory: String) {
        categoryDao.editCategoryAndMigration(oldCategory, newCategory)
    }

    override suspend fun deleteCategoryAndMigration(category: Category) {
        categoryDao.deleteCategoryAndMigration(category)
    }

    override suspend fun getNotesWithEnabledReminders(): List<Note> {
        return noteDao.getNotesWithEnabledReminders()
    }

    override fun getReminderHistoryFlow(): Flow<List<com.example.data.local.entity.ReminderHistoryEntry>> {
        return reminderHistoryDao.getAllHistoryFlow()
    }

    override suspend fun insertReminderHistory(entry: com.example.data.local.entity.ReminderHistoryEntry) {
        reminderHistoryDao.insert(entry)
    }

    override suspend fun clearReminderHistory() {
        reminderHistoryDao.clearAllHistory()
    }
}
