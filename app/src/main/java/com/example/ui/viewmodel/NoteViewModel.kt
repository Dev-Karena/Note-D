package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Note
import com.example.data.local.entity.Category
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NoteUiState(
    val activeNotes: List<Note> = emptyList(),
    val archivedNotes: List<Note> = emptyList(),
    val currentNote: Note? = null,
    val searchResults: List<Note> = emptyList(),
    val searchQuery: String = "",
    val searchHistory: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReminderUiState(
    val id: Int = -1,
    val reminderTime: Long? = null,
    val repeatType: com.example.data.local.entity.RepeatType = com.example.data.local.entity.RepeatType.NONE,
    val isReminderEnabled: Boolean = false,
    val validationError: String? = null,
    val isSavedSuccessfully: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface ReminderEvent {
    data class ToggleReminder(val enabled: Boolean) : ReminderEvent
    data class SetDateTime(val timeInMillis: Long) : ReminderEvent
    data class ChangeRepeatType(val repeatType: com.example.data.local.entity.RepeatType) : ReminderEvent
    data class ValidateAndSaveReminder(val context: android.content.Context, val noteId: Int) : ReminderEvent
    object ClearReminder : ReminderEvent
    object ClearValidationError : ReminderEvent
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class NoteViewModel(
    private val repository: NoteRepository,
    private val sharedPrefs: android.content.SharedPreferences? = null,
    private val securePrefs: com.example.security.SecurePreferencesManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    private val _reminderUiState = MutableStateFlow(ReminderUiState())
    val reminderUiState: StateFlow<ReminderUiState> = _reminderUiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")

    private val _pendingNoteIdToOpen = MutableStateFlow<Int?>(null)
    val pendingNoteIdToOpen: StateFlow<Int?> = _pendingNoteIdToOpen.asStateFlow()

    fun setPendingNoteId(id: Int?) {
        _pendingNoteIdToOpen.value = id
    }

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun setLockState(locked: Boolean) {
        _isLocked.value = locked
    }

    private val _isDarkModeEnabled = MutableStateFlow(sharedPrefs?.getBoolean("pref_dark_mode", false) ?: false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    private val _isGridViewEnabled = MutableStateFlow(sharedPrefs?.getBoolean("pref_grid_view", true) ?: true)
    val isGridViewEnabled: StateFlow<Boolean> = _isGridViewEnabled.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(sharedPrefs?.getBoolean("pref_notifications", true) ?: true)
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(sharedPrefs?.getBoolean("pref_biometric", false) ?: false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Secure Preferences Toggles (EncryptedSharedPreferences)
    private val _isGlobalRemindersEnabled = MutableStateFlow(securePrefs?.isGlobalRemindersEnabled() ?: true)
    val isGlobalRemindersEnabled: StateFlow<Boolean> = _isGlobalRemindersEnabled.asStateFlow()

    private val _snoozeDurationMinutes = MutableStateFlow(securePrefs?.getSnoozeDurationMinutes() ?: 15)
    val snoozeDurationMinutes: StateFlow<Int> = _snoozeDurationMinutes.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(securePrefs?.isSoundEnabled() ?: true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    private val _isVibrationEnabled = MutableStateFlow(securePrefs?.isVibrationEnabled() ?: true)
    val isVibrationEnabled: StateFlow<Boolean> = _isVibrationEnabled.asStateFlow()

    val reminderHistory: StateFlow<List<com.example.data.local.entity.ReminderHistoryEntry>> = repository.getReminderHistoryFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setDarkModeEnabled(enabled: Boolean) {
        _isDarkModeEnabled.value = enabled
        sharedPrefs?.edit()?.putBoolean("pref_dark_mode", enabled)?.apply()
    }

    fun setGridViewEnabled(enabled: Boolean) {
        _isGridViewEnabled.value = enabled
        sharedPrefs?.edit()?.putBoolean("pref_grid_view", enabled)?.apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
        sharedPrefs?.edit()?.putBoolean("pref_notifications", enabled)?.apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        sharedPrefs?.edit()?.putBoolean("pref_biometric", enabled)?.apply()
    }

    fun setGlobalRemindersEnabled(enabled: Boolean) {
        _isGlobalRemindersEnabled.value = enabled
        securePrefs?.setGlobalRemindersEnabled(enabled)
    }

    fun setSnoozeDurationMinutes(minutes: Int) {
        _snoozeDurationMinutes.value = minutes
        securePrefs?.setSnoozeDurationMinutes(minutes)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        securePrefs?.setSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _isVibrationEnabled.value = enabled
        securePrefs?.setVibrationEnabled(enabled)
    }

    fun clearReminderHistory() {
        viewModelScope.launch {
            repository.clearReminderHistory()
        }
    }

    init {
        // Load initial search history
        loadSearchHistory()

        // Observe categories and pre-populate if empty
        viewModelScope.launch {
            repository.getAllCategories().collect { dbCategories ->
                if (dbCategories.isEmpty()) {
                    val defaults = listOf("Work", "Personal", "Study", "Ideas")
                    defaults.forEach { name ->
                        repository.insertCategory(Category(name = name))
                    }
                } else {
                    _uiState.update { it.copy(categories = dbCategories) }
                }
            }
        }

        // Observe active notes and category selection combine
        viewModelScope.launch {
            combine(
                _selectedCategory,
                repository.getActiveNotes()
            ) { category, notes ->
                val filtered = if (category == "All") {
                    notes
                } else {
                    notes.filter { it.category.equals(category, ignoreCase = true) }
                }
                category to filtered
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to load active notes") }
            }
            .collect { (category, filteredNotes) ->
                _uiState.update { 
                    it.copy(
                        selectedCategory = category,
                        activeNotes = filteredNotes,
                        error = null
                    ) 
                }
            }
        }

        // Observe archived notes
        viewModelScope.launch {
            repository.getArchivedNotes()
                .catch { e ->
                    _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to load archived notes") }
                }
                .collect { archived ->
                    _uiState.update { it.copy(archivedNotes = archived, error = null) }
                }
        }

        // Search flow with 300ms debounce
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    val searchFlow = if (query.isBlank()) {
                        flowOf(emptyList())
                    } else {
                        repository.searchNotes(query)
                    }
                    searchFlow.map { results -> query to results }
                }
                .catch { e ->
                    _uiState.update { it.copy(error = e.localizedMessage ?: "Failed to perform search") }
                }
                .collect { (query, results) ->
                    _uiState.update { 
                        it.copy(
                            searchResults = results,
                            error = null
                        ) 
                    }
                }
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun createCategory(name: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            onError("Category name cannot be empty")
            return
        }
        if (_uiState.value.categories.any { it.name.equals(trimmed, ignoreCase = true) }) {
            onError("Category already exists")
            return
        }
        viewModelScope.launch {
            try {
                repository.insertCategory(Category(name = trimmed))
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to create category")
            }
        }
    }

    fun editCategory(oldName: String, newName: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            onError("Category name cannot be empty")
            return
        }
        if (oldName.equals(trimmed, ignoreCase = true)) {
            onSuccess() // No-op
            return
        }
        if (_uiState.value.categories.any { it.name.equals(trimmed, ignoreCase = true) }) {
            onError("Category details conflict with an existing category")
            return
        }
        viewModelScope.launch {
            try {
                repository.editCategoryAndMigration(oldName, trimmed)
                if (_selectedCategory.value == oldName) {
                    _selectedCategory.value = trimmed
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to update category")
            }
        }
    }

    fun deleteCategory(category: Category, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteCategoryAndMigration(category)
                if (_selectedCategory.value == category.name) {
                    _selectedCategory.value = "All"
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete category")
            }
        }
    }

    fun loadNote(noteId: Int) {
        if (noteId == -1) {
            _uiState.update { it.copy(currentNote = null) }
            _reminderUiState.value = ReminderUiState(id = -1)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val note = repository.getNoteById(noteId)
            _uiState.update { it.copy(currentNote = note, isLoading = false) }
            if (note != null) {
                _reminderUiState.value = ReminderUiState(
                    id = note.id,
                    reminderTime = note.reminderTime,
                    repeatType = note.repeatType,
                    isReminderEnabled = note.isReminderEnabled
                )
            }
        }
    }

    fun validateReminderState(state: ReminderUiState): String? {
        if (state.isReminderEnabled) {
            val time = state.reminderTime
            if (time == null) {
                return "Reminder time cannot be empty when enabled"
            }
            if (time <= System.currentTimeMillis()) {
                return "Reminder time must be in the future"
            }
        }
        return null
    }

    fun onReminderEvent(event: ReminderEvent) {
        when (event) {
            is ReminderEvent.ToggleReminder -> {
                _reminderUiState.update { 
                    it.copy(
                        isReminderEnabled = event.enabled,
                        reminderTime = if (event.enabled && it.reminderTime == null) {
                            System.currentTimeMillis() + 3600_000 // 1 hour default
                        } else if (!event.enabled) {
                            null
                        } else {
                            it.reminderTime
                        },
                        repeatType = if (event.enabled) it.repeatType else com.example.data.local.entity.RepeatType.NONE
                    )
                }
            }
            is ReminderEvent.SetDateTime -> {
                _reminderUiState.update { it.copy(reminderTime = event.timeInMillis) }
            }
            is ReminderEvent.ChangeRepeatType -> {
                _reminderUiState.update { it.copy(repeatType = event.repeatType) }
            }
            is ReminderEvent.ValidateAndSaveReminder -> {
                validateAndSaveReminder(event.context, event.noteId)
            }
            ReminderEvent.ClearReminder -> {
                _reminderUiState.update { 
                    it.copy(
                        reminderTime = null,
                        repeatType = com.example.data.local.entity.RepeatType.NONE,
                        isReminderEnabled = false,
                        validationError = null,
                        isSavedSuccessfully = false
                    )
                }
            }
            ReminderEvent.ClearValidationError -> {
                _reminderUiState.update { it.copy(validationError = null) }
            }
        }
    }

    private fun validateAndSaveReminder(context: android.content.Context, noteId: Int) {
        val currentState = _reminderUiState.value
        val errorMsg = validateReminderState(currentState)
        if (errorMsg != null) {
            _reminderUiState.update { it.copy(validationError = errorMsg) }
            return
        }
        
        viewModelScope.launch {
            _reminderUiState.update { it.copy(isLoading = true) }
            try {
                val existingNote = repository.getNoteById(noteId)
                if (existingNote != null) {
                    val updatedNote = existingNote.copy(
                        reminderTime = if (currentState.isReminderEnabled) currentState.reminderTime else null,
                        repeatType = if (currentState.isReminderEnabled) currentState.repeatType else com.example.data.local.entity.RepeatType.NONE,
                        isReminderEnabled = currentState.isReminderEnabled
                    )
                    repository.updateNote(updatedNote)
                    
                    if (currentState.isReminderEnabled && currentState.reminderTime != null) {
                        com.example.reminder.ReminderScheduler.schedule(
                            context = context,
                            noteId = noteId,
                            timeInMillis = currentState.reminderTime,
                            repeatType = currentState.repeatType
                        )
                    } else {
                        com.example.reminder.ReminderScheduler.cancel(context, noteId)
                    }
                    _reminderUiState.update { it.copy(isSavedSuccessfully = true, isLoading = false) }
                } else {
                    _reminderUiState.update { it.copy(validationError = "Note not found", isLoading = false) }
                }
            } catch (e: Exception) {
                _reminderUiState.update { it.copy(validationError = e.localizedMessage ?: "Failed to save reminder", isLoading = false) }
            }
        }
    }

    fun saveNote(
        context: android.content.Context,
        id: Int,
        title: String,
        content: String,
        category: String,
        color: Int,
        reminderTime: Long? = null,
        isPinned: Boolean = false,
        repeatType: com.example.data.local.entity.RepeatType = com.example.data.local.entity.RepeatType.NONE,
        isReminderEnabled: Boolean = false,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (title.isBlank() && content.isBlank()) {
                    onError("Note title and content cannot both be empty.")
                    return@launch
                }
                val currentTime = System.currentTimeMillis()
                var savedId = id
                if (id == -1) {
                    // Insert new note
                    val newNote = Note(
                        title = title,
                        content = content,
                        category = category.ifBlank { "Personal" },
                        createdAt = currentTime,
                        updatedAt = currentTime,
                        color = color,
                        reminderTime = reminderTime,
                        isPinned = isPinned,
                        isArchived = false,
                        repeatType = repeatType,
                        isReminderEnabled = isReminderEnabled
                    )
                    savedId = repository.insertNote(newNote).toInt()
                } else {
                    // Update existing note
                    val existingNote = repository.getNoteById(id)
                    if (existingNote != null) {
                        val updatedNote = existingNote.copy(
                            title = title,
                            content = content,
                            category = category.ifBlank { existingNote.category },
                            updatedAt = currentTime,
                            color = color,
                            reminderTime = reminderTime,
                            isPinned = isPinned,
                            repeatType = repeatType,
                            isReminderEnabled = isReminderEnabled
                        )
                        repository.updateNote(updatedNote)
                    }
                }

                // Schedule or cancel precise WorkManager reminder
                if (reminderTime != null && isReminderEnabled) {
                    com.example.reminder.ReminderScheduler.schedule(
                        context = context,
                        noteId = savedId,
                        timeInMillis = reminderTime,
                        repeatType = repeatType
                    )
                } else {
                    com.example.reminder.ReminderScheduler.cancel(context, savedId)
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to save note due to database error.")
            }
        }
    }

    fun deleteNote(context: android.content.Context, note: Note, onError: (String) -> Unit = {}, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                com.example.reminder.ReminderScheduler.cancel(context, note.id)
                repository.deleteNote(note)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to delete note due to database error.")
            }
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updatePinStatus(note.id, !note.isPinned)
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.updateArchiveStatus(note.id, !note.isArchived)
        }
    }

    private fun loadSearchHistory() {
        val historyStr = sharedPrefs?.getString("search_history_v1", "") ?: ""
        val historyList = if (historyStr.isBlank()) emptyList() else historyStr.split("\u001f")
        _uiState.update { it.copy(searchHistory = historyList) }
    }

    fun addSearchToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value.searchHistory.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val updated = current.take(10)
        saveSearchHistory(updated)
    }

    fun deleteSearchFromHistory(query: String) {
        val current = _uiState.value.searchHistory.toMutableList()
        current.remove(query)
        saveSearchHistory(current)
    }

    fun clearSearchHistory() {
        saveSearchHistory(emptyList())
    }

    private fun saveSearchHistory(history: List<String>) {
        _uiState.update { it.copy(searchHistory = history) }
        sharedPrefs?.edit()?.putString("search_history_v1", history.joinToString("\u001f"))?.apply()
    }

    class Factory(
        private val repository: NoteRepository,
        private val sharedPrefs: android.content.SharedPreferences? = null,
        private val securePrefs: com.example.security.SecurePreferencesManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
                return NoteViewModel(repository, sharedPrefs, securePrefs) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
