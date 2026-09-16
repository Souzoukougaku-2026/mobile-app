package com.example.keyframeplayer

import android.net.Uri
import androidx.room.TypeConverter
import java.util.UUID

class RoomTypeConverters {
    // ① UUID型 を String型 に変換してデータベースに保存する
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    // ② データベースから読み込んだ String型 を UUID型 に戻す
    @TypeConverter
    fun toUUID(value: String?): UUID? {
        return value?.let { UUID.fromString(it) }
    }

    // ③ Uri型 を String型 に変換して保存する
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    // ④ データベースから読み込んだ String型 を Uri型 に戻す
    @TypeConverter
    fun toUri(value: String?): android.net.Uri? {
        return value?.let { android.net.Uri.parse(it) }
    }
}