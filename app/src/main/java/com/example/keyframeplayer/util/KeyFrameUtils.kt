package com.example.keyframeplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import android.graphics.BitmapFactory

object KeyFrameUtils {

    /**
     * 動画からキーフレームを1枚ずつ取得し、コールバックに渡す。
     * Bitmapはコールバック実行後にこの関数内で解放する。
     */
    suspend fun forEachKeyframe(
        context: Context,
        uri: Uri,
        limit: Int = 10,
        onFrame: suspend (currentFrame: Int, totalFrames: Int, timeUs: Long, bitmap: Bitmap) -> Unit
    ) {
        val timesUs = getKeyframeTimes(context, uri)
        if (timesUs.isEmpty()) return

        val targetTimes = timesUs.take(limit)
        val totalFrames = targetTimes.size
        val retriever = MediaMetadataRetriever()

        try {
            try {
                retriever.setDataSource(context, uri)
            } catch (e: Exception) {
                e.printStackTrace()
                return
            }

            for ((index, timeUs) in targetTimes.withIndex()) {
                val currentFrame = index + 1
                val bitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: continue

                try {
                    onFrame(currentFrame, totalFrames, timeUs, bitmap)
                } finally {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
        } finally {
            retriever.release()
        }
    }

    /**
     * 動画内のキーフレームのタイムスタンプを取得する。
     */
    private fun getKeyframeTimes(context: Context, uri: Uri): List<Long> {
        val extractor = MediaExtractor()

        return try {
            extractor.setDataSource(context, uri, null)

            var videoTrackIndex = -1

            for (i in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(i)
                    .getString(MediaFormat.KEY_MIME)

                if (mime?.startsWith("video/") == true) {
                    videoTrackIndex = i
                    break
                }
            }

            if (videoTrackIndex == -1) {
                return emptyList()
            }

            extractor.selectTrack(videoTrackIndex)

            val times = mutableListOf<Long>()

            extractor.seekTo(
                0L,
                MediaExtractor.SEEK_TO_CLOSEST_SYNC
            )

            var currentSampleTime = extractor.sampleTime

            if (currentSampleTime >= 0) {
                times.add(currentSampleTime)
            }

            while (currentSampleTime >= 0) {
                extractor.seekTo(
                    currentSampleTime + 1,
                    MediaExtractor.SEEK_TO_NEXT_SYNC
                )

                val nextSampleTime = extractor.sampleTime

                if (nextSampleTime < 0 || nextSampleTime <= currentSampleTime) {
                    break
                }

                times.add(nextSampleTime)
                currentSampleTime = nextSampleTime
            }

            times
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            extractor.release()
        }
    }

    /**
     * Bitmapをアプリ内部ストレージにPNG形式で保存し、絶対パスを返す。
     */
    fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap
    ): String? {
        val directory = File(context.filesDir, "images")

        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }

        val file = File(directory, "${UUID.randomUUID()}.png")

        return try {
            FileOutputStream(file).use { outputStream ->
                val saved = bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    outputStream
                )

                if (!saved) {
                    throw IOException("Bitmapの保存に失敗しました")
                }
            }

            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            file.delete()
            null
        }
    }

    /**
     * 基準日時にマイクロ秒を加算し、ミリ秒のLong値で返す。
     */
    fun addMicrosecondsToLong(
        baseTimeText: String,
        microsecondsToAdd: Long,
        pattern: String = "yyyy/MM/dd HH:mm:ss.SSS"
    ): Long {
        return try {
            val localDateTime = try {
                LocalDateTime.parse(baseTimeText, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"))
            } catch (e: Exception) {
                LocalDateTime.parse(baseTimeText, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
            }

            val calculatedDateTime = localDateTime.plus(
                microsecondsToAdd,
                ChronoUnit.MICROS
            )

            calculatedDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * PNG画像の絶対パスからBitmapを取得する
     *
     * @param absolutePath 画像ファイルの絶対パス
     * @return デコードされたBitmap（失敗時、またはファイルが存在しない場合はnull）
     */
    fun loadBitmapFromPath(absolutePath: String): Bitmap? {
        val file = File(absolutePath)

        // ファイルが存在し、かつ読み取り可能かチェック
        if (!file.exists() || !file.isFile) {
            return null
        }

        // パスからBitmapを生成して返す
        return BitmapFactory.decodeFile(file.absolutePath)
    }

}
