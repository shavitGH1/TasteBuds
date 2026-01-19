package com.sandg.tastebuds.dao

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sandg.tastebuds.models.Ingredient

object Converters {

    private val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun fromIngredientsList(value: List<Ingredient>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    @JvmStatic
    fun toIngredientsList(value: String?): List<Ingredient>? {
        if (value == null) return listOf()
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    @JvmStatic
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    @JvmStatic
    fun toStringList(value: String?): List<String>? {
        if (value == null) return listOf()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}

