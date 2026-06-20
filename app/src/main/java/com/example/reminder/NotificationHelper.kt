package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.security.SecurePreferencesManager
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.ReminderHistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {
    const val CHANNEL_ID = "note_reminders_channel"
    private const val CHANNEL_NAME = "Note Reminders"
    private const val CHANNEL_DESC = "Notifications for scheduled note reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, noteId: Int, title: String, content: String) {
        val securePrefs = SecurePreferencesManager(context)
        
        // 1. Global reminders toggle check
        if (!securePrefs.isGlobalRemindersEnabled()) {
            Log.d("NotificationHelper", "All reminders are globally disabled. Suppressing notification for Note $noteId")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("note_id", noteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId, // unique requestCode to avoid collisions
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Broadcast PendingIntent
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE
            putExtra("note_id", noteId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            noteId + 100000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Broadcast PendingIntent
        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DISMISS
            putExtra("note_id", noteId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            noteId + 200000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Try using drawable/ic_launcher_foreground (flat vector drawable), fallback to android system alarm icon if needed
        val smallIcon = try {
            com.example.R.drawable.ic_launcher_foreground
        } catch (_: Exception) {
            android.R.drawable.ic_dialog_info
        }

        val soundEnabled = securePrefs.isSoundEnabled()
        val vibrationEnabled = securePrefs.isVibrationEnabled()
        val snoozeMins = securePrefs.getSnoozeDurationMinutes()

        var defaults = 0
        if (soundEnabled) {
            defaults = defaults or NotificationCompat.DEFAULT_SOUND
        }
        if (vibrationEnabled) {
            defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title.ifBlank { "Note Reminder" })
            .setContentText(content.ifBlank { "You have a scheduled note waiting." })
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(defaults)
            .setSilent(!soundEnabled)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(context.getColor(android.R.color.holo_blue_light))
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze (${snoozeMins}m)",
                snoozePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissPendingIntent
            )

        if (vibrationEnabled) {
            notification.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
            notification.setVibrate(null)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(noteId, notification.build())

        // 2. Insert "TRIGGERED" history record
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val historyEntry = ReminderHistoryEntry(
                    noteId = noteId,
                    noteTitle = title.ifBlank { "Note Reminder" },
                    timestamp = System.currentTimeMillis(),
                    status = "TRIGGERED"
                )
                db.reminderHistoryDao().insert(historyEntry)
                Log.d("NotificationHelper", "Successfully recorded TRIGGERED event for Note $noteId")
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error logging reminder history: ${e.message}")
            }
        }
    }

    fun cancelNotification(context: Context, noteId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(noteId)
    }
}
