package com.example.keyframeplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_images")
data class CropImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cropImage: ByteArray,
    val className: String,
    val score: Float,
    val color: String,
    val timeStampReal: Long,
    val timeStampFile: Long,
    val keyFrame: Boolean,
    val movieAddress: String
)