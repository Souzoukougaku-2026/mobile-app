package com.example.keyframeplayer

import android.net.Uri
import androidx.room.TypeConverter
import com.example.keyframeplayer.core.domain.model.ImageColor
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

    // ⑤ ImageColor (Enum) を String型 に変換して保存する
    @TypeConverter
    fun fromImageColor(color: ImageColor?): String? {
        return color?.name
    }

    // ⑥ データベースから読み込んだ String型 を ImageColor (Enum) に戻す
    @TypeConverter
    fun toImageColor(value: String?): ImageColor? {
        return value?.let {
            runCatching { ImageColor.valueOf(it) }.getOrNull()
        }
    }
}
