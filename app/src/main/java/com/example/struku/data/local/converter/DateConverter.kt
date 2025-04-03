package com.example.struku.data.local.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converter for Date objects in Room database
 */
class DateConverter {
    /**
     * Convert from Date to Long for database storage
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Convert from Long to Date for use in application
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return if (timestamp != null && timestamp > 0) Date(timestamp) else null
    }
}
