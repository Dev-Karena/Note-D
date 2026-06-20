package com.example.reminder

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.entity.RepeatType
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    /**
     * Schedules a background reminder using WorkManager according to Modern Android Best Practices.
     * Supports both One-Time and Periodic work triggers with precise initial delays.
     */
    fun schedule(
        context: Context,
        noteId: Int,
        timeInMillis: Long,
        repeatType: RepeatType = RepeatType.NONE
    ) {
        val now = System.currentTimeMillis()
        val initialDelay = (timeInMillis - now).coerceAtLeast(0L)

        // Ensure we cancel any prev scheduled work before rescheduling to avoid overlaps or duplicate triggers
        cancel(context, noteId)

        try {
            val workManager = WorkManager.getInstance(context)

            if (repeatType == RepeatType.NONE) {
                // One-Time Reminder Setup via OneTimeWorkRequest
                Log.d(TAG, "Scheduling one-time reminder via WorkManager for note $noteId with initial delay of ${initialDelay}ms")
                val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInputData(workDataOf("note_id" to noteId))
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag("reminder_tag_$noteId")
                    .build()

                workManager.enqueueUniqueWork(
                    "reminder_onetime_$noteId",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
            } else {
                // Recurring Reminder Setup via PeriodicWorkRequest
                val intervalInDays = when (repeatType) {
                    RepeatType.DAILY -> 1L
                    RepeatType.WEEKLY -> 7L
                    RepeatType.MONTHLY -> 30L // Approx 30 days for WorkManager periodic intervals
                    else -> 1L
                }

                Log.d(TAG, "Scheduling periodic reminder ($repeatType) via WorkManager for note $noteId starting in ${initialDelay}ms, repeating every $intervalInDays days")
                
                val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(intervalInDays, TimeUnit.DAYS)
                    .setInputData(workDataOf("note_id" to noteId))
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag("reminder_tag_$noteId")
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    "reminder_periodic_$noteId",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to schedule reminder via WorkManager: ${t.message}", t)
        }
    }

    /**
     * Cancels any active scheduled reminders for the given Note ID.
     */
    fun cancel(context: Context, noteId: Int) {
        Log.d(TAG, "Cancelling scheduled reminders for note $noteId via WorkManager")
        try {
            val workManager = WorkManager.getInstance(context)
            
            // Cancel all associated work by unique name pattern
            workManager.cancelUniqueWork("reminder_onetime_$noteId")
            workManager.cancelUniqueWork("reminder_periodic_$noteId")
            workManager.cancelAllWorkByTag("reminder_tag_$noteId")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to cancel reminder via WorkManager (commonly in tests): ${t.message}")
        }
    }
}
