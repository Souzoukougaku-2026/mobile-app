package com.example.mikke.core.util

import android.graphics.Bitmap

/**
 * 画像の (x, y) を左上として、width × height の範囲を切り抜く。
 *
 * 座標・サイズが画像の範囲に収まっていない場合は例外を投げるため、
 * 呼び出し側で事前に範囲内へ丸めておくこと。
 */
fun Bitmap.cropImage(x: Int, y: Int, width: Int, height: Int): Bitmap {
    // check the coordinates
    require(0 <= x && x <= this.width - 1) {
        "you passed invalid x coordinate of the left-top corner"
    }
    require(0 <= y && y <= this.height - 1) {
        "you passed invalid y coordinate of the left-top corner"
    }
    require(width > 0) { "width should be more than 0" }
    require(height > 0) { "height should be more than 0" }

    require(x + width <= this.width) {
        "invalid width"
    }
    require(y + height <= this.height) {
        "invalid height"
    }

    return Bitmap.createBitmap(this, x, y, width, height)
}
