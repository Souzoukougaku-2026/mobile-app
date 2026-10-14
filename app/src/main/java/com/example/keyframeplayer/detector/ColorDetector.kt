package com.example.keyframeplayer.detector

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.example.keyframeplayer.core.domain.model.ImageColor

fun Bitmap.detectDominantColor(): ImageColor {
    val palette = Palette.from(this)
        .generate()

    val rgb = palette.dominantSwatch?.rgb ?: return ImageColor.White

    return classifyColor(rgb)
}

private fun classifyColor(rgb: Int): ImageColor {
    val hsv = FloatArray(3)
    Color.colorToHSV(rgb, hsv)

    val h = hsv[0] // 0.0 - 360.0 (色相: Hue)
    val s = hsv[1] // 0.0 - 1.0 (彩度: Saturation)
    val v = hsv[2] // 0.0 - 1.0 (明度: Value)

    return when {
        // 無彩色の判定（黒・白・灰色）
        v < 0.15 -> ImageColor.Black
        v > 0.95 -> ImageColor.White
        s < 0.2 -> ImageColor.Gray

        // 有彩色の判定（色相環の角度に基づく）
        h in 0.0..15.0 || h in 330.0..360.0 -> ImageColor.Red
        h in 15.0..45.0 -> ImageColor.Orange
        h in 45.0..70.0 -> ImageColor.Yellow
        h in 70.0..160.0 -> ImageColor.Green
        h in 160.0..250.0 -> ImageColor.Blue
        h in 250.0..290.0 -> ImageColor.Purple
        h in 290.0..330.0 -> ImageColor.Pink
        else -> ImageColor.White
    }
}
