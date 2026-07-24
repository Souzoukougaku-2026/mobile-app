package com.example.a2026souzou.feature.movie.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import com.example.a2026souzou.core.data.database.entity.CropImageEntity
import com.example.a2026souzou.core.data.database.entity.KeyFrameEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class VideoFrameExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getKeyframeTimes(uri: Uri): List<Long> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val intervalUs = 60 * 1000_000L // 1分間隔

            val times = mutableListOf<Long>()
            var currentUs = 0L
            while (currentUs < durationMs * 1000) {
                times.add(currentUs)
                currentUs += intervalUs
            }
            times
        } catch (e: Exception) {
            emptyList()
        } finally {
            retriever.release()
        }
    }

    fun extractFrames(uri: Uri, timesUs: List<Long>, limit: Int = 100): List<Pair<Long, Bitmap>> {
        val retriever = MediaMetadataRetriever()
        val items = mutableListOf<Pair<Long, Bitmap>>()
        try {
            retriever.setDataSource(context, uri)
            for (timeUs in timesUs.take(limit)) {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 512, 512)
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                bitmap?.let { items.add(Pair(timeUs, it)) }
            }
        } catch (e: Exception) {
        } finally {
            retriever.release()
        }
        return items
    }

    fun saveFrameAsEntities(bitmap: Bitmap, timeUs: Long, videoUri: Uri): Pair<KeyFrameEntity, CropImageEntity> {
        val keyFrameId = UUID.randomUUID()
        val filename = "frame_${timeUs}_${keyFrameId}.jpg"
        val file = File(context.filesDir, filename)

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }

        val keyFrame = KeyFrameEntity(
            id = keyFrameId,
            realTime = System.currentTimeMillis(),
            fileTime = timeUs / 1000,
            keyFramePath = file.absolutePath,
            moviePath = videoUri.toString()
        )

        val cropImage = CropImageEntity(
            id = UUID.randomUUID(),
            className = "Keyframe",
            score = 1.0,
            color = 0, // White
            bboxLeft = 0, bboxTop = 0, bboxRight = bitmap.width, bboxBottom = bitmap.height,
            realTime = keyFrame.realTime,
            idKeyFrame = keyFrameId
        )

        return Pair(keyFrame, cropImage)
    }
}