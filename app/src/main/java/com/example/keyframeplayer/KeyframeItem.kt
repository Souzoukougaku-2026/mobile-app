package com.example.keyframeplayer

import android.graphics.Bitmap

data class KeyframeItem(
    val timeUs: Long,
    val bitmap: Bitmap
)