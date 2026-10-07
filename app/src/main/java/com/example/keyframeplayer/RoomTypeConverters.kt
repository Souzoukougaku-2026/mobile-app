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

    // ③ Uri型 を String型 に変換して保存する（💡KeyFrameEntityの14行目対策）
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    // ④ データベースから読み込んだ String型 を Uri型 に戻す（💡KeyFrameEntityの14行目対策）
    @TypeConverter
    fun toUri(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    // ⑤ BaseColor (Enum) を String型 に変換して保存する
    @TypeConverter
    fun fromBaseColor(color: BaseColor?): String? {
        return color?.name
    }

    // ⑥ データベースから読み込んだ String型 を BaseColor (Enum) に戻す
    @TypeConverter
    fun toBaseColor(value: String?): BaseColor? {
        return value?.let {
            runCatching { BaseColor.valueOf(it) }.getOrNull()
        }
    }

    // ⑦ BPoint を カンマ区切りのString型 "ulX,ulY,lrX,lrY" に変換して保存する
    @TypeConverter
    fun fromBPoint(point: BPoint?): String? {
        return point?.let { "${it.ulX},${it.ulY},${it.lrX},${it.lrY}" }
    }

    // ⑧ データベースから読み込んだ String型 を BPoint に戻す
    @TypeConverter
    fun toBPoint(value: String?): BPoint? {
        if (value.isNullOrEmpty()) return null
        val parts = value.split(",")
        if (parts.size != 4) return null
        return runCatching {
            BPoint(
                ulX = parts[0].toFloat(),
                ulY = parts[1].toFloat(),
                lrX = parts[2].toFloat(),
                lrY = parts[3].toFloat()
            )
        }.getOrNull()
    }
}
