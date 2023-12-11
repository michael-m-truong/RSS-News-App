package com.bignerdranch.android.newsapp.database

import androidx.room.TypeConverter
import com.bignerdranch.android.newsapp.models.ReadTimeOption
import java.util.Date

class NewsFeedTypeConverters {
    @TypeConverter
    fun fromDate(date: Date): Long {
        return date.time
    }
    @TypeConverter
    fun toDate(millisSinceEpoch: Long): Date {
        return Date(millisSinceEpoch)
    }

    @TypeConverter
    fun fromStringList(stringList: List<String>): String {
        return stringList.joinToString(",")
    }

    @TypeConverter
    fun toStringList(string: String): List<String> {
        return string.split(",").map { it.trim() }
    }

    @TypeConverter
    fun fromReadTimeOptions(readTimeOptions: List<ReadTimeOption>): List<String> {
        return readTimeOptions.map { it.ordinal.toString() }
    }

    @TypeConverter
    fun toReadTimeOptions(ordinalList: List<String>): List<ReadTimeOption> {
        return try {
            ordinalList.map { ReadTimeOption.values()[it.toInt()] }
        } catch (e: NumberFormatException) {
            // Log the error or handle it as needed
            e.printStackTrace()

            // Provide a fallback value (empty list in this case)
            emptyList()
        }
    }

    @TypeConverter
    fun fromSourceOption(sourceOption: HashMap<String, Boolean>): String {
        return sourceOption.entries.joinToString(",") { (key, value) -> "$key=$value" }
    }

    @TypeConverter
    fun toSourceOption(sourceOptionString: String): HashMap<String, Boolean> {
        val entries = sourceOptionString.split(",").map { it.trim().split("=") }
        return HashMap(entries.map { (key, value) -> key to value.toBoolean() }.toMap())
    }

}