package com.example.keyframeplayer.detector

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.example.keyframeplayer.data.BaseColor

fun Bitmap.detectDominantColor(): BaseColor {
    val palette = Palette.from(this)
        .generate()

    val rgb = palette.dominantSwatch?.rgb ?: return BaseColor.UNKNOWN

    return classifyColor(rgb)
}

private fun classifyColor(rgb: Int): BaseColor {
    val hsv = FloatArray(3)
    Color.colorToHSV(rgb, hsv)

    val h = hsv[0] // 0.0 - 360.0 (色相: Hue)
    val s = hsv[1] // 0.0 - 1.0 (彩度: Saturation)
    val v = hsv[2] // 0.0 - 1.0 (明度: Value)

    return when {
        // 無彩色の判定（黒・白・灰色）
        v < 0.15 -> BaseColor.BLACK
        v > 0.95 -> BaseColor.WHITE
        s < 0.2 -> BaseColor.GRAY

        // 有彩色の判定（色相環の角度に基づく）
        h in 0.0..15.0 || h in 330.0..360.0 -> BaseColor.RED
        h in 15.0..45.0 -> BaseColor.ORANGE
        h in 45.0..70.0 -> BaseColor.YELLOW
        h in 70.0..160.0 -> BaseColor.GREEN
        h in 160.0..250.0 -> BaseColor.BLUE
        h in 250.0..290.0 -> BaseColor.PURPLE
        h in 290.0..330.0 -> BaseColor.PINK
        else -> BaseColor.UNKNOWN
    }
}
