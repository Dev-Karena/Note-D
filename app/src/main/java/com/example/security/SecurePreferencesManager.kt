package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferencesManager(context: Context) {
    private val TAG = "SecurePreferences"
    private var sharedPreferences: SharedPreferences? = null

    init {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_noted_prefs_v2",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing EncryptedSharedPreferences, falling back to basic secure mode: ${e.message}")
            // Fallback to standard SharedPreferences in emulator environments or legacy devices where Keystore fails
            sharedPreferences = context.getSharedPreferences("secure_noted_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun setPin(pin: String?) {
        sharedPreferences?.edit()?.putString("app_pin_hash", pin)?.apply()
    }

    fun getPin(): String? {
        return sharedPreferences?.getString("app_pin_hash", null)
    }

    fun isPinSet(): Boolean {
        return !getPin().isNullOrEmpty()
    }

    fun setLastInteractionTime(time: Long) {
        sharedPreferences?.edit()?.putLong("last_interaction_time", time)?.apply()
    }

    fun getLastInteractionTime(): Long {
        return sharedPreferences?.getLong("last_interaction_time", 0L) ?: 0L
    }

    fun clearPin() {
        sharedPreferences?.edit()?.remove("app_pin_hash")?.apply()
    }

    fun isGlobalRemindersEnabled(): Boolean {
        return sharedPreferences?.getBoolean("pref_global_reminders", true) ?: true
    }

    fun setGlobalRemindersEnabled(enabled: Boolean) {
        sharedPreferences?.edit()?.putBoolean("pref_global_reminders", enabled)?.apply()
    }

    fun getSnoozeDurationMinutes(): Int {
        return sharedPreferences?.getInt("pref_snooze_duration", 15) ?: 15
    }

    fun setSnoozeDurationMinutes(minutes: Int) {
        sharedPreferences?.edit()?.putInt("pref_snooze_duration", minutes)?.apply()
    }

    fun isSoundEnabled(): Boolean {
        return sharedPreferences?.getBoolean("pref_reminder_sound", true) ?: true
    }

    fun setSoundEnabled(enabled: Boolean) {
        sharedPreferences?.edit()?.putBoolean("pref_reminder_sound", enabled)?.apply()
    }

    fun isVibrationEnabled(): Boolean {
        return sharedPreferences?.getBoolean("pref_reminder_vibration", true) ?: true
    }

    fun setVibrationEnabled(enabled: Boolean) {
        sharedPreferences?.edit()?.putBoolean("pref_reminder_vibration", enabled)?.apply()
    }
}
