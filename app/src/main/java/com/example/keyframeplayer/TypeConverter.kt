package com.example.keyframeplayer

import android.net.Uri
import androidx.room.TypeConverter
import com.example.keyframeplayer.data.BaseColor
import com.example.keyframeplayer.data.BPoint
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

    @TypeConverter
    fun fromUri(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    fun toUri(value: String?): Uri? = value?.let { Uri.parse(it) }

    @TypeConverter
    fun fromBaseColor(color: BaseColor?): String? = color?.name

    @TypeConverter
    fun toBaseColor(value: String?): BaseColor? = value?.let {
        try {
            enumValueOf<BaseColor>(it)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromBPoint(point: BPoint?): String? = point?.let { "${it.ulX},${it.ulY},${it.lrX},${it.lrY}" }

    @TypeConverter
    fun toBPoint(value: String?): BPoint? = value?.split(",")?.let {
        if (it.size == 4) BPoint(it[0].toFloat(), it[1].toFloat(), it[2].toFloat(), it[3].toFloat()) else null
    }
}