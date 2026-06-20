package com.example.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.RepeatType

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val noteId = inputData.getInt("note_id", -1)
        if (noteId == -1) {
            Log.e(TAG, "Worker triggered but note_id is missing.")
            return Result.failure()
        }

        Log.d(TAG, "ReminderWorker starting work for Note ID: $noteId")

        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val note = db.noteDao().getNoteById(noteId)

            if (note != null && note.isReminderEnabled && !note.isArchived) {
                // Trigger notification in the helper
                NotificationHelper.showNotification(
                    context = applicationContext,
                    noteId = note.id,
                    title = note.title,
                    content = note.content
                )

                // For OneTimeWorkRequest, it naturally stops.
                // But wait, if this was a One-Time reminder (RepeatType.NONE),
                // we should update its reminder state to disabled so it's not shown as active.
                if (note.repeatType == RepeatType.NONE) {
                    val updatedNote = note.copy(
                        isReminderEnabled = false
                    )
                    db.noteDao().updateNote(updatedNote)
                } else if (note.repeatType == RepeatType.MONTHLY) {
                    // For monthly, since month durations vary, sometimes one-time reschedule is preferred.
                    // But if it is PeriodicWorkRequest, WorkManager runs it on a 30-day fixed periodic interval,
                    // which is adequate, or we could manually roll the next trigger date. Let's update the note's
                    // stored reminderTime to the next timestamp so the UI is kept perfectly in sync!
                    val nextTime = calculateNextOccurrence(note.reminderTime ?: System.currentTimeMillis(), note.repeatType)
                    if (nextTime != null) {
                        val updatedNote = note.copy(reminderTime = nextTime)
                        db.noteDao().updateNote(updatedNote)
                    }
                } else {
                    // DAILY/WEEKLY: also update the reminderTime in the DB so next occurrences are shown correctly in UI.
                    val nextTime = calculateNextOccurrence(note.reminderTime ?: System.currentTimeMillis(), note.repeatType)
                    if (nextTime != null) {
                        val updatedNote = note.copy(reminderTime = nextTime)
                        db.noteDao().updateNote(updatedNote)
                    }
                }

                Log.d(TAG, "Notification triggered and handled successfully for Note ID: $noteId")
                Result.success()
            } else {
                Log.d(TAG, "Note not found, archived, or reminder disabled: ID = $noteId")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing ReminderWorker: ${e.message}", e)
            Result.retry()
        }
    }

    private fun calculateNextOccurrence(startTime: Long, repeatType: RepeatType): Long? {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startTime
        }
        val now = java.util.Calendar.getInstance()
        
        while (cal.timeInMillis <= now.timeInMillis) {
            when (repeatType) {
                RepeatType.DAILY -> cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                RepeatType.WEEKLY -> cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
                RepeatType.MONTHLY -> cal.add(java.util.Calendar.MONTH, 1)
                RepeatType.NONE -> return null
            }
        }
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "ReminderWorker"
    }
}
