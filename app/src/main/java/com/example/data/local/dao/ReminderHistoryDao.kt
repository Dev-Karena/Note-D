package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ReminderHistoryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ReminderHistoryEntry)

    @Query("SELECT * FROM reminder_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<ReminderHistoryEntry>>

    @Query("DELETE FROM reminder_history")
    suspend fun clearAllHistory()
}
