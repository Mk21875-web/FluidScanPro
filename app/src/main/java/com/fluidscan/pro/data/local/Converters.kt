package com.fluidscan.pro.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Room type converters. Lists are stored as compact JSON to keep the schema flat. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList()
        else Json.decodeFromString(ListSerializer(String.serializer()), value)
}
