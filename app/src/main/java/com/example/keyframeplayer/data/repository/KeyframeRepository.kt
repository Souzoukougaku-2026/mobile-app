package com.example.keyframeplayer.data.repository

import android.content.Context
import android.net.Uri
import com.example.keyframeplayer.KeyframeItem
import com.example.keyframeplayer.data.local.CropImageDao
import com.example.keyframeplayer.data.local.CropImageEntity
import com.example.keyframeplayer.data.video.KeyframeExtractor
import com.example.keyframeplayer.util.ImageConverter

class KeyframeRepository(
    private val extractor: KeyframeExtractor,
    private val dao: CropImageDao
) {

    suspend fun loadAndSaveKeyframes(
        context: Context,
        uri: Uri
    ): List<KeyframeItem> {

        val times = extractor.getKeyframeTimes(context, uri)
        val items = extractor.getKeyframeItems(context, uri, times)

        items.forEach { item ->

            // 🔥 フレーム画像をファイル保存
            val keyframePath = ImageConverter.saveBitmapToInternalStorage(
                context,
                item.bitmap,
                "keyframe_${item.timeUs}.png"
            )

            val entity = CropImageEntity(
                cropImagePath = keyframePath,   // 今回は同じでもOK
                className = "unknown",
                score = 0f,
                color = "unknown",
                timeStampReal = System.currentTimeMillis(),
                timeStampFile = item.timeUs,
                keyFramePath = keyframePath,    // ← 修正
                moviePath = uri.toString()
            )

            dao.insert(entity)
        }

        return items
    }

}