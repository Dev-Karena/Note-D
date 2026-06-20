package com.example.data.local.database

import androidx.room.TypeConverter
import com.example.data.local.entity.RepeatType

class Converters {
    @TypeConverter
    fun fromRepeatType(repeatType: RepeatType): String {
        return repeatType.name
    }

    @TypeConverter
    fun toRepeatType(value: String): RepeatType {
        return try {
            RepeatType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            RepeatType.NONE
        }
    }
}
