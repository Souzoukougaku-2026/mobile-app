package com.example.keyframeplayer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.japaneseClassName

/**
 * bbox で切り抜いた画像。
 * キーフレーム全体ではなく検出範囲を表示し、余白は背面 (黒背景) に任せる。
 * リスト画面のサムネイルと動画画面下部のカードで共用する。
 *
 * @param cropPaddingRatio 切り抜き範囲を各辺に広げる割合 (0.2f = 幅・高さの20%ずつ)
 */
@Composable
fun BboxThumbnail(
    detection: CropImage,
    cropPaddingRatio: Float = 0.2f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(
        detection.id,
        detection.keyFramePath,
        detection.bboxLeft,
        detection.bboxTop,
        detection.bboxRight,
        detection.bboxBottom,
        cropPaddingRatio
    ) {
        ImageRequest.Builder(context)
            .data(detection.keyFramePath)
            // bbox は元画像の絶対座標なので、縮小デコードせず元サイズで切り抜く
            .size(Size.ORIGINAL)
            .transformations(
                BboxCropTransformation(
                    bboxLeft = detection.bboxLeft,
                    bboxTop = detection.bboxTop,
                    bboxRight = detection.bboxRight,
                    bboxBottom = detection.bboxBottom,
                    paddingRatio = cropPaddingRatio
                )
            )
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = detection.japaneseClassName,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
