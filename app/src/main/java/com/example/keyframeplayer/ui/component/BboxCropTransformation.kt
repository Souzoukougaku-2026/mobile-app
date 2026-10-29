package com.example.keyframeplayer.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import coil.size.Dimension
import coil.size.Size
import coil.transform.Transformation
import com.example.keyframeplayer.core.util.cropImage

/**
 * キーフレーム画像を bbox の範囲で切り抜き、表示サイズ (正方形) に収まるよう
 * 縦横の大きいほうの辺に合わせてリサイズし、余った領域を黒で埋めた Bitmap を返す
 * Coil 用 Transformation。
 *
 * bbox はキーフレーム画像に対する絶対ピクセル座標 (left, top, right, bottom)。
 * DB の値が画像範囲から外れている場合に備え、変換前に範囲内へ丸める。
 */
class BboxCropTransformation(
    private val bboxLeft: Int,
    private val bboxTop: Int,
    private val bboxRight: Int,
    private val bboxBottom: Int
) : Transformation {

    // 同じキーフレームから複数の検出があってもキャッシュが混ざらないよう、bbox をキーに含める
    override val cacheKey: String =
        "BboxCropTransformation($bboxLeft,$bboxTop,$bboxRight,$bboxBottom)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val startNs = System.nanoTime()

        // bbox を画像範囲内へ丸める (幅・高さが最低 1px は残るようにする)
        val left = bboxLeft.coerceIn(0, input.width - 1)
        val top = bboxTop.coerceIn(0, input.height - 1)
        val right = bboxRight.coerceIn(left + 1, input.width)
        val bottom = bboxBottom.coerceIn(top + 1, input.height)

        val cropped = input.cropImage(
            x = left,
            y = top,
            width = right - left,
            height = bottom - top
        )

        // 出力サイズ: Coil から要求サイズ (ピクセル指定) が来ればそれを使い、
        // 未指定なら切り抜きの大きいほうの辺を一辺とする正方形にする
        val targetWidth = (size.width as? Dimension.Pixels)?.px ?: maxOf(cropped.width, cropped.height)
        val targetHeight = (size.height as? Dimension.Pixels)?.px ?: maxOf(cropped.width, cropped.height)

        // 縦横の大きいほうの辺に合わせるスケール (全体が収まる contain)
        val scale = minOf(
            targetWidth.toFloat() / cropped.width,
            targetHeight.toFloat() / cropped.height
        )
        val scaledWidth = cropped.width * scale
        val scaledHeight = cropped.height * scale
        val dstLeft = (targetWidth - scaledWidth) / 2f
        val dstTop = (targetHeight - scaledHeight) / 2f

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        canvas.drawBitmap(
            cropped,
            null,
            RectF(dstLeft, dstTop, dstLeft + scaledWidth, dstTop + scaledHeight),
            Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
        )

        // 最適化検討のための計測ログ (ステップ3で実測値を見て要否を判断する)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        Log.d(
            "BboxCropTransformation",
            "transform: crop=${cropped.width}x${cropped.height} -> ${targetWidth}x$targetHeight, " +
                "elapsed=${"%.2f".format(elapsedMs)}ms"
        )

        return output
    }
}
