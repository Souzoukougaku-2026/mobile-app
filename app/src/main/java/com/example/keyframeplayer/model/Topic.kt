package com.example.keyframeplayer.model

import androidx.annotation.DrawableRes
import com.example.keyframeplayer.R
import java.util.Locale

data class Topic(
    val class_name: String,
    val fileTime: Long, // マイクロ秒
    val imagePath: String? = null,
    val moviePath: android.net.Uri? = null,
    @DrawableRes val imageRes: Int = R.drawable.ic_launcher_foreground,
    val imageColor: Int = 0,
    val realTime: Long = 0L,
    val bboxLeft: Float = 0f,
    val bboxTop: Float = 0f,
    val bboxRight: Float = 500f,
    val bboxBottom: Float = 500f
) {
    // マイクロ秒を 00:00:00 形式に変換
    val formattedTime: String
        get() {
            val totalSeconds = fileTime / 1_000_000
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        }
}
