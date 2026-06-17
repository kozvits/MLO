package com.mlo.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromLongList(value: String): List<Long> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    @TypeConverter
    fun toLongList(list: List<Long>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun fromStringSet(value: String): Set<String> {
        if (value.isEmpty()) return emptySet()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    @TypeConverter
    fun toStringSet(set: Set<String>): String {
        return set.joinToString(",")
    }

    @TypeConverter
    fun fromMapStringString(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun toMapStringString(map: Map<String, String>): String {
        return gson.toJson(map)
    }
}
