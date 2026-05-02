package com.example.keyframeplayer.util

import android.graphics.Bitmap


import android.content.Context
import java.io.File
import java.io.FileOutputStream



object ImageConverter {

    fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): String {
        val file = File(context.filesDir, fileName)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file.absolutePath
    }
}