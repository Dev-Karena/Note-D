package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NoteApplication
import com.example.data.local.entity.RepeatType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? NoteApplication
                    if (app == null) {
                        Log.e(TAG, "Application context is not NoteApplication. Cannot restore reminders.")
                        return@launch
                    }
                    
                    val repository = app.noteRepository
                    val notesWithReminders = repository.getNotesWithEnabledReminders()
                    Log.d(TAG, "Found ${notesWithReminders.size} notes with enabled reminders to restore.")

                    val now = System.currentTimeMillis()

                    for (note in notesWithReminders) {
                        val reminderTime = note.reminderTime ?: continue
                        
                        if (note.repeatType == RepeatType.NONE) {
                            if (reminderTime > now) {
                                Log.d(TAG, "Restoring future one-time reminder for Note ${note.id}")
                                ReminderScheduler.schedule(context, note.id, reminderTime, RepeatType.NONE)
                            } else {
                                Log.d(TAG, "Skipping past one-time reminder for Note ${note.id}. Disabling.")
                                val updatedNote = note.copy(isReminderEnabled = false)
                                repository.updateNote(updatedNote)
                            }
                        } else {
                            // Recurring reminder
                            val targetTime = if (reminderTime <= now) {
                                val nextTime = calculateNextOccurrence(reminderTime, note.repeatType)
                                if (nextTime != null) {
                                    Log.d(TAG, "Recalculating missed recurring reminder for Note ${note.id} from $reminderTime to $nextTime")
                                    val updatedNote = note.copy(reminderTime = nextTime)
                                    repository.updateNote(updatedNote)
                                    nextTime
                                } else {
                                    reminderTime
                                }
                            } else {
                                reminderTime
                            }

                            Log.d(TAG, "Restoring recurring reminder for Note ${note.id} at $targetTime")
                            ReminderScheduler.schedule(context, note.id, targetTime, note.repeatType)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring reminders on boot: ${e.message}", e)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    private fun calculateNextOccurrence(startTime: Long, repeatType: RepeatType): Long? {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startTime
        }
        val now = Calendar.getInstance()
        
        while (cal.timeInMillis <= now.timeInMillis) {
            when (repeatType) {
                RepeatType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RepeatType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatType.MONTHLY -> cal.add(Calendar.MONTH, 1)
                RepeatType.NONE -> return null
            }
        }
        return cal.timeInMillis
    }
}
