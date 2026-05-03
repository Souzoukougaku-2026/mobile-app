package com.example.keyframeplayer.data

data class CropImage(
    val id: String,
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
