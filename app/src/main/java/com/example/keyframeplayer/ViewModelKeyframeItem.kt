package com.example.keyframeplayer

import android.graphics.Bitmap
import android.net.Uri

data class KeyframeItem(
    val timeUs: Long,
    val bitmap: Bitmap,
    val uri: Uri? = null
)