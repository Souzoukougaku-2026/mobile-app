package com.example.mikke.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

object KeyFrameUtils {

    /**
     * 高速化ターゲットサイズ（物体検出・表示用に最適化された解像度）
     */
    private const val TARGET_FRAME_SIZE = 640

    /**
     * キーフレーム間の最小サンプリング間隔 (300ms = 300,000us)
     */
    private const val MIN_KEYFRAME_INTERVAL_US = 300_000L

    /**
     * 動画からキーフレームを1枚ずつ高速に取得し、コールバックに渡す。
     * Bitmapはコールバック実行後にこの関数内で解放する。
     */
    suspend fun forEachKeyframe(
        context: Context,
        uri: Uri,
        limit: Int = 86400,
        onFrame: suspend (currentFrame: Int, totalFrames: Int, timeUs: Long, bitmap: Bitmap) -> Unit
    ) {
        val rawTimesUs = getKeyframeTimes(context, uri)
        if (rawTimesUs.isEmpty()) return

        // 0.3秒未満の過度に連続するキーフレームをフィルタリングして無駄な処理を削減
        val timesUs = mutableListOf<Long>()
        var lastAddedUs = -1L
        for (t in rawTimesUs) {
            if (lastAddedUs == -1L || (t - lastAddedUs) >= MIN_KEYFRAME_INTERVAL_US) {
                timesUs.add(t)
                lastAddedUs = t
            }
        }

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

                // 高速化: 最初から指定サイズにスケーリングしてデコード (API 27+)
                val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    try {
                        retriever.getScaledFrameAtTime(
                            timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            TARGET_FRAME_SIZE,
                            TARGET_FRAME_SIZE
                        )
                    } catch (e: Exception) {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }

                if (bitmap == null) continue

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
     * Bitmapをアプリ内部ストレージにJPEG形式 (クオリティ 85) で高速保存し、絶対パスを返す。
     */
    fun saveBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap
    ): String? {
        val directory = File(context.filesDir, "images")

        if (!directory.exists() && !directory.mkdirs()) {
            return null
        }

        val file = File(directory, "${UUID.randomUUID()}.jpg")

        return try {
            BufferedOutputStream(FileOutputStream(file)).use { outputStream ->
                val saved = bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
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
     * 画像の絶対パスからBitmapを取得する
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
