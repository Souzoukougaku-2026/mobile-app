package com.example.keyframeplayer.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_images")
data class CropImage(
    @PrimaryKey val id: String,
    val cropImagePath: String,
    val className: String,
    val score: Float,
    val color: String,
    val timestampRealTime: Long,
    val timestampFileTime: Long,
    val keyFrame: Int,
    val movieAddress: String
)

data class CropImageTimestamp(
    val id: String,
    val className: String,
    val timestampRealTime: Long,
    val timestampFileTime: Long,
    val movieAddress: String
)
