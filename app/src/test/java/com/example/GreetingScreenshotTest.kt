package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.entity.Note
import com.example.data.repository.NoteRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NoteViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val fakeRepository = object : NoteRepository {
    override fun getActiveNotes() = flowOf(emptyList<Note>())
    override fun getArchivedNotes() = flowOf(emptyList<Note>())
    override suspend fun getNoteById(id: Int): Note? = null
    override fun searchNotes(query: String) = flowOf(emptyList<Note>())
    override fun getNotesByCategory(category: String) = flowOf(emptyList<Note>())
    override suspend fun insertNote(note: Note): Long = 0L
    override suspend fun updateNote(note: Note) {}
    override suspend fun deleteNote(note: Note) {}
    override suspend fun updatePinStatus(id: Int, isPinned: Boolean) {}
    override suspend fun updateArchiveStatus(id: Int, isArchived: Boolean) {}
    override fun getAllCategories() = flowOf(emptyList<com.example.data.local.entity.Category>())
    override suspend fun insertCategory(category: com.example.data.local.entity.Category) {}
    override suspend fun deleteCategory(category: com.example.data.local.entity.Category) {}
    override suspend fun editCategoryAndMigration(oldCategory: String, newCategory: String) {}
    override suspend fun deleteCategoryAndMigration(category: com.example.data.local.entity.Category) {}
    override suspend fun getNotesWithEnabledReminders(): List<Note> = emptyList()
    override fun getReminderHistoryFlow() = flowOf(emptyList<com.example.data.local.entity.ReminderHistoryEntry>())
    override suspend fun insertReminderHistory(entry: com.example.data.local.entity.ReminderHistoryEntry) {}
    override suspend fun clearReminderHistory() {}
  }

  @Test
  fun greeting_screenshot() {
    val viewModel = NoteViewModel(fakeRepository)
    composeTestRule.setContent { 
        MyApplicationTheme { 
            HomeScreen(viewModel = viewModel) 
        } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
