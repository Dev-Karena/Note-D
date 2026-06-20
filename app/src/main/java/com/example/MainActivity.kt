package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.ui.navigation.AppNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.screens.LockScreen
import com.example.security.SecurePreferencesManager

class MainActivity : FragmentActivity() {
  private lateinit var viewModel: NoteViewModel
  private val securePrefsManager by lazy { SecurePreferencesManager(this) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as NoteApplication
    val sharedPrefs = app.getSharedPreferences("noted_prefs", android.content.Context.MODE_PRIVATE)
    val factory = NoteViewModel.Factory(app.noteRepository, sharedPrefs, securePrefsManager)
    viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

    // Lock app on launch if passcode PIN is configured
    if (securePrefsManager.isPinSet()) {
      viewModel.setLockState(true)
    }

    handleIntent(intent)

    setContent {
      val isDarkTheme by viewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
      val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()

      MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize()
        ) {
          if (isLocked) {
            LockScreen(
              viewModel = viewModel,
              onUnlockSuccess = {
                viewModel.setLockState(false)
              }
            )
          } else {
            val navController = rememberNavController()
            AppNavGraph(navController = navController, viewModel = viewModel)
          }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    // Capture background timestamp
    securePrefsManager.setLastInteractionTime(System.currentTimeMillis())
  }

  override fun onStart() {
    super.onStart()
    // Session timeout verification: if more than 30 seconds have passed in background, trigger automatic vault lock
    val isBiometricEnabled = viewModel.isBiometricEnabled.value
    val isPinSet = securePrefsManager.isPinSet()
    if (isBiometricEnabled || isPinSet) {
      val lastTime = securePrefsManager.getLastInteractionTime()
      if (lastTime > 0L) {
        val elapsed = System.currentTimeMillis() - lastTime
        if (elapsed > 30_000L) {
          viewModel.setLockState(true)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    intent?.let {
      val noteId = it.getIntExtra("note_id", -1)
      if (noteId != -1) {
        viewModel.setPendingNoteId(noteId)
      }
    }
  }
}
