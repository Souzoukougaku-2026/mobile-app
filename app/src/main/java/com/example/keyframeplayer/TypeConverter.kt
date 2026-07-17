package com.example.keyframeplayer

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
}