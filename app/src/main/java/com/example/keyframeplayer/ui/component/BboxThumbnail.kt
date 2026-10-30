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
    cropImage: CropImage,
    cropPaddingRatio: Float = 0.2f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(
        cropImage.id,
        cropImage.keyFramePath,
        cropImage.bboxLeft,
        cropImage.bboxTop,
        cropImage.bboxRight,
        cropImage.bboxBottom,
        cropPaddingRatio
    ) {
        ImageRequest.Builder(context)
            .data(cropImage.keyFramePath)
            // bbox は元画像の絶対座標なので、縮小デコードせず元サイズで切り抜く
            .size(Size.ORIGINAL)
            .transformations(
                BboxCropTransformation(
                    bboxLeft = cropImage.bboxLeft,
                    bboxTop = cropImage.bboxTop,
                    bboxRight = cropImage.bboxRight,
                    bboxBottom = cropImage.bboxBottom,
                    paddingRatio = cropPaddingRatio
                )
            )
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = cropImage.japaneseClassName,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
