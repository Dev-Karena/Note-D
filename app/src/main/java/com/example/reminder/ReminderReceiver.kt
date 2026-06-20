package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    private val TAG = "ReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("note_id", -1)
        if (noteId == -1) {
            Log.e(TAG, "Received alarm or notification action, but note_id is missing.")
            return
        }

        val action = intent.action
        Log.d(TAG, "ReminderReceiver received action: $action for Note ID: $noteId")

        if (action == ACTION_SNOOZE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val note = db.noteDao().getNoteById(noteId)
                    if (note != null) {
                        val securePrefs = com.example.security.SecurePreferencesManager(context)
                        val snoozeMins = securePrefs.getSnoozeDurationMinutes()
                        val snoozeTime = System.currentTimeMillis() + snoozeMins * 60 * 1000L
                        val updatedNote = note.copy(
                            reminderTime = snoozeTime,
                            isReminderEnabled = true
                        )
                        db.noteDao().updateNote(updatedNote)
                        
                        // Schedule snooze work
                        ReminderScheduler.schedule(context, noteId, snoozeTime, com.example.data.local.entity.RepeatType.NONE)
                        Log.d(TAG, "Successfully snoozed Note $noteId for $snoozeMins minutes.")

                        // Record SNOOZED event
                        val historyEntry = com.example.data.local.entity.ReminderHistoryEntry(
                            noteId = noteId,
                            noteTitle = note.title,
                            timestamp = System.currentTimeMillis(),
                            status = "SNOOZED"
                        )
                        db.reminderHistoryDao().insert(historyEntry)
                    }
                    NotificationHelper.cancelNotification(context, noteId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error snoozing note reminder: ${e.message}")
                } finally {
                    pendingResult?.finish()
                }
            }
            return
        }

        if (action == ACTION_DISMISS) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val note = db.noteDao().getNoteById(noteId)
                    if (note != null) {
                        if (note.repeatType == com.example.data.local.entity.RepeatType.NONE) {
                            // Non-recurring: disable reminder since it has finished
                            val updatedNote = note.copy(
                                isReminderEnabled = false
                            )
                            db.noteDao().updateNote(updatedNote)
                            Log.d(TAG, "Dismissed and disabled one-time reminder for Note $noteId.")
                        }

                        // Record DISMISSED event
                        val historyEntry = com.example.data.local.entity.ReminderHistoryEntry(
                            noteId = noteId,
                            noteTitle = note.title,
                            timestamp = System.currentTimeMillis(),
                            status = "DISMISSED"
                        )
                        db.reminderHistoryDao().insert(historyEntry)
                    }
                    NotificationHelper.cancelNotification(context, noteId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error dismissing note reminder: ${e.message}")
                } finally {
                    pendingResult?.finish()
                }
            }
            return
        }

        Log.d(TAG, "Alarm triggered for Note ID: $noteId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val note = db.noteDao().getNoteById(noteId)
                if (note != null && note.reminderTime != null && !note.isArchived) {
                    NotificationHelper.showNotification(
                        context = context,
                        noteId = note.id,
                        title = note.title,
                        content = note.content
                    )

                    // Reschedule if it's a recurring reminder
                    if (note.isReminderEnabled && note.repeatType != com.example.data.local.entity.RepeatType.NONE) {
                        val nextTime = calculateNextOccurrence(note.reminderTime, note.repeatType)
                        if (nextTime != null) {
                            val updatedNote = note.copy(reminderTime = nextTime)
                            db.noteDao().updateNote(updatedNote)
                            ReminderScheduler.schedule(context, note.id, nextTime, note.repeatType)
                            Log.d(TAG, "Successfully rescheduled note $noteId for next occurrence at $nextTime")
                        }
                    }
                } else {
                    Log.d(TAG, "Note not found, archived, or reminder cleared: ID = $noteId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching note for reminder: ${e.message}")
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private fun calculateNextOccurrence(startTime: Long, repeatType: com.example.data.local.entity.RepeatType): Long? {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startTime
        }
        val now = java.util.Calendar.getInstance()
        
        while (cal.timeInMillis <= now.timeInMillis) {
            when (repeatType) {
                com.example.data.local.entity.RepeatType.DAILY -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                com.example.data.local.entity.RepeatType.WEEKLY -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                com.example.data.local.entity.RepeatType.MONTHLY -> cal.add(java.util.Calendar.MONTH, 1)
                com.example.data.local.entity.RepeatType.NONE -> return null
            }
        }
        return cal.timeInMillis
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.reminder.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.example.reminder.ACTION_DISMISS"
    }
}
