package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.Category
import com.example.data.local.entity.Note
import com.example.data.repository.NoteRepositoryImpl
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.ReminderUiState
import com.example.ui.viewmodel.ReminderEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoteViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var viewModel: NoteViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // In-memory Room database for fast deterministic unit tests
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = NoteRepositoryImpl(db.noteDao(), db.categoryDao(), db.reminderHistoryDao())
        
        // Setup ViewModel
        val sharedPrefs = context.getSharedPreferences("test_noted_prefs", Context.MODE_PRIVATE)
        viewModel = NoteViewModel(repository, sharedPrefs)
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun waitTasksToSettle() {
        for (i in 1..5) {
            kotlinx.coroutines.delay(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    @Test
    fun `saveNote with empty title and content triggers validation error`() = runBlocking {
        var errorMessage: String? = null
        var isSuccessCalled = false

        viewModel.saveNote(
            context = ApplicationProvider.getApplicationContext(),
            id = -1,
            title = "   ",
            content = "",
            category = "Personal",
            color = 0xFFFFFFFF.toInt(),
            onError = { err -> errorMessage = err },
            onSuccess = { isSuccessCalled = true }
        )

        waitTasksToSettle()

        assertNotNull(errorMessage)
        assertEquals("Note title and content cannot both be empty.", errorMessage)
        assertFalse(isSuccessCalled)
    }

    @Test
    fun `saveNote with content is successful`() = runBlocking {
        var errorMessage: String? = null
        var isSuccessCalled = false

        viewModel.saveNote(
            context = ApplicationProvider.getApplicationContext(),
            id = -1,
            title = "A Great Idea",
            content = "This is a unit test note body detail.",
            category = "Work",
            color = 0xFF00FF00.toInt(),
            onError = { err -> 
                println("DEBUG_SAVE_NOTE_ERROR_1: $err")
                errorMessage = err 
            },
            onSuccess = { isSuccessCalled = true }
        )

        waitTasksToSettle()

        assertNull(errorMessage)
        assertTrue(isSuccessCalled)

        // Verify it was correctly stored and fetched
        val notes = repository.getActiveNotes().first()
        assertEquals(1, notes.size)
        assertEquals("A Great Idea", notes[0].title)
        assertEquals("Work", notes[0].category)
    }

    @Test
    fun `createCategory with empty name trigger error`() = runBlocking {
        var errorMessage: String? = null
        var isSuccess = false

        viewModel.createCategory(
            name = "   ",
            onSuccess = { isSuccess = true },
            onError = { err -> errorMessage = err }
        )

        waitTasksToSettle()

        assertNotNull(errorMessage)
        assertEquals("Category name cannot be empty", errorMessage)
        assertFalse(isSuccess)
    }

    @Test
    fun `createCategory with existing duplicate name trigger error`() = runBlocking {
        var error1: String? = null
        var success1 = false
        
        waitTasksToSettle()

        viewModel.createCategory(
            name = "UniqueCategoryUnderTesting",
            onSuccess = { success1 = true },
            onError = { err -> error1 = err }
        )
        waitTasksToSettle()

        var errorMessage: String? = null
        var isSuccess = false

        viewModel.createCategory(
            name = "UniqueCategoryUnderTesting",
            onSuccess = { isSuccess = true },
            onError = { err -> errorMessage = err }
        )

        waitTasksToSettle()

        assertNotNull(errorMessage)
        assertEquals("Category already exists", errorMessage)
        assertFalse(isSuccess)
    }

    @Test
    fun `saveNote with recurring reminder properties successfully persists`() = runBlocking {
        var isSuccessCalled = false

        viewModel.saveNote(
            context = ApplicationProvider.getApplicationContext(),
            id = -1,
            title = "Recurring Meeting",
            content = "Keep this weekly status updated.",
            category = "Work",
            color = 0xFF0000FF.toInt(),
            reminderTime = 1716912000000L,
            isPinned = true,
            repeatType = com.example.data.local.entity.RepeatType.WEEKLY,
            isReminderEnabled = true,
            onError = { 
                println("DEBUG_SAVE_NOTE_ERROR_2: $it")
                fail("Save failed with $it") 
            },
            onSuccess = { isSuccessCalled = true }
        )

        waitTasksToSettle()

        assertTrue(isSuccessCalled)

        // Verify the properties in database
        val notes = repository.getActiveNotes().first()
        assertEquals(1, notes.size)
        val note = notes[0]
        assertEquals("Recurring Meeting", note.title)
        assertEquals(com.example.data.local.entity.RepeatType.WEEKLY, note.repeatType)
        assertTrue(note.isReminderEnabled)
        assertEquals(1716912000000L, note.reminderTime)
    }

    @Test
    fun `default reminder state yields empty inputs`() {
        val reminderState = viewModel.reminderUiState.value
        assertEquals(-1, reminderState.id)
        assertNull(reminderState.reminderTime)
        assertEquals(com.example.data.local.entity.RepeatType.NONE, reminderState.repeatType)
        assertFalse(reminderState.isReminderEnabled)
        assertNull(reminderState.validationError)
    }

    @Test
    fun `ReminderEvent ToggleReminder enables and defaults duration`() {
        viewModel.onReminderEvent(ReminderEvent.ToggleReminder(true))
        val state = viewModel.reminderUiState.value
        assertTrue(state.isReminderEnabled)
        assertNotNull(state.reminderTime)
        assertTrue(state.reminderTime!! - System.currentTimeMillis() >= 0)

        viewModel.onReminderEvent(ReminderEvent.ToggleReminder(false))
        val inactiveState = viewModel.reminderUiState.value
        assertFalse(inactiveState.isReminderEnabled)
        assertNull(inactiveState.reminderTime)
        assertEquals(com.example.data.local.entity.RepeatType.NONE, inactiveState.repeatType)
    }

    @Test
    fun `ReminderEvent ChangeRepeatType updates state correctly`() {
        viewModel.onReminderEvent(ReminderEvent.ToggleReminder(true))
        viewModel.onReminderEvent(ReminderEvent.ChangeRepeatType(com.example.data.local.entity.RepeatType.DAILY))
        assertEquals(com.example.data.local.entity.RepeatType.DAILY, viewModel.reminderUiState.value.repeatType)

        viewModel.onReminderEvent(ReminderEvent.ChangeRepeatType(com.example.data.local.entity.RepeatType.MONTHLY))
        assertEquals(com.example.data.local.entity.RepeatType.MONTHLY, viewModel.reminderUiState.value.repeatType)
    }

    @Test
    fun `ReminderEvent SetDateTime updates timestamp`() {
        val targetTime = System.currentTimeMillis() + 50000L
        viewModel.onReminderEvent(ReminderEvent.SetDateTime(targetTime))
        assertEquals(targetTime, viewModel.reminderUiState.value.reminderTime)
    }

    @Test
    fun `Reminder validation handles historical dates and active bounds`() {
        // Correct/future time
        val validState = ReminderUiState(
            isReminderEnabled = true,
            reminderTime = System.currentTimeMillis() + 10_000,
            repeatType = com.example.data.local.entity.RepeatType.DAILY
        )
        assertNull(viewModel.validateReminderState(validState))

        // Historical time -> in past
        val invalidTimeState = ReminderUiState(
            isReminderEnabled = true,
            reminderTime = System.currentTimeMillis() - 5000,
            repeatType = com.example.data.local.entity.RepeatType.DAILY
        )
        assertEquals("Reminder time must be in the future", viewModel.validateReminderState(invalidTimeState))

        // Empty time when enabled
        val emptyTimeState = ReminderUiState(
            isReminderEnabled = true,
            reminderTime = null
        )
        assertEquals("Reminder time cannot be empty when enabled", viewModel.validateReminderState(emptyTimeState))
    }

    @Test
    fun `ReminderEvent ClearReminder rests state to defaults`() {
        viewModel.onReminderEvent(ReminderEvent.ToggleReminder(true))
        viewModel.onReminderEvent(ReminderEvent.ChangeRepeatType(com.example.data.local.entity.RepeatType.WEEKLY))
        
        viewModel.onReminderEvent(ReminderEvent.ClearReminder)
        
        val state = viewModel.reminderUiState.value
        assertFalse(state.isReminderEnabled)
        assertNull(state.reminderTime)
        assertEquals(com.example.data.local.entity.RepeatType.NONE, state.repeatType)
        assertNull(state.validationError)
    }
    // Refresh tests
}
