package com.example.gpssportmap.data.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

class Converters {

    @TypeConverter
    fun fromBitmap(bmp: Bitmap?): ByteArray? {
        if (bmp == null) return null
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? {
        if (bytes == null) return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // Add other converters here if you need them for other types
    // For example, converting a List<String> to a single String:
    /*
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(',')?.map { it }
    }
    */
}
