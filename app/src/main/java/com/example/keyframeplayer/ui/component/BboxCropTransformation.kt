package com.example.keyframeplayer.ui.component

import android.graphics.Bitmap
import android.util.Log
import coil.size.Size
import coil.transform.Transformation
import com.example.keyframeplayer.core.util.cropImage

/**
 * キーフレーム画像を bbox の範囲で切り抜くだけの Coil 用 Transformation。
 *
 * リサイズや余白の黒埋めは行わない (表示側の ContentScale.Fit と背景色に任せる)。
 * bbox はキーフレーム画像に対する絶対ピクセル座標 (left, top, right, bottom) のため、
 * 呼び出し側のリクエストで元サイズのままデコードすること (Size.ORIGINAL 指定)。
 *
 * @param paddingRatio bbox を各辺に広げる割合 (0.1f なら幅・高さの10%ずつ外側へ)。
 *                     広げた結果や DB の値が画像範囲から外れても、切り抜き前に
 *                     範囲内へ丸められるので安全。
 */
class BboxCropTransformation(
    private val bboxLeft: Int,
    private val bboxTop: Int,
    private val bboxRight: Int,
    private val bboxBottom: Int,
    private val paddingRatio: Float = 0f
) : Transformation {

    // 同じキーフレームから複数の検出があってもキャッシュが混ざらないよう、bbox と拡張率をキーに含める
    override val cacheKey: String =
        "BboxCropTransformation($bboxLeft,$bboxTop,$bboxRight,$bboxBottom,pad=$paddingRatio)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val startNs = System.nanoTime()

        // bbox を paddingRatio の分だけ各辺へ広げる (左右は幅基準、上下は高さ基準)
        val padX = ((bboxRight - bboxLeft) * paddingRatio).toInt()
        val padY = ((bboxBottom - bboxTop) * paddingRatio).toInt()
        val expandedLeft = bboxLeft - padX
        val expandedTop = bboxTop - padY
        val expandedRight = bboxRight + padX
        val expandedBottom = bboxBottom + padY

        // 拡張後の範囲を画像範囲内へ丸める (幅・高さが最低 1px は残るようにする)
        val left = expandedLeft.coerceIn(0, input.width - 1)
        val top = expandedTop.coerceIn(0, input.height - 1)
        val right = expandedRight.coerceIn(left + 1, input.width)
        val bottom = expandedBottom.coerceIn(top + 1, input.height)

        val cropped = input.cropImage(
            x = left,
            y = top,
            width = right - left,
            height = bottom - top
        )

        // 最適化検討のための計測ログ (ステップ3で実測値を見て要否を判断する)
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0
        Log.d(
            "BboxCropTransformation",
            "transform: src=${input.width}x${input.height} -> crop=${cropped.width}x${cropped.height}, " +
                "elapsed=${"%.2f".format(elapsedMs)}ms"
        )

        return cropped
    }
}
