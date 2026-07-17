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

/**
 * キーフレーム（時間とBitmap）を保持するシンプルなデータクラス
 */
data class KeyFrameResult(
    val timeUs: Long,
    val bitmap: Bitmap
)

object KeyFrameUtils {


    /**
     * 動画のURIからキーフレーム情報を一括で取得するメイン関数
     *
     * @param context コンテキスト
     * @param videoUri 動画のUri
     * @param limit 抽出する最大枚数（デフォルト: 10枚）
     */

    fun extractKeyframes(context: Context, videoUri: Uri, limit: Int = 10): List<KeyFrameResult> {
        // 1. まず動画から同期フレーム（キーフレーム）のタイムスタンプをすべて集める
        val allTimesUs = getKeyframeTimes(context, videoUri)

        // 2. 指定された上限数（limit）に絞って、各タイムスタンプのBitmap（画像）を取得する
        return getKeyframeItems(context, videoUri, allTimesUs, limit)
    }

    /**
     * 動画内のすべてのキーフレーム（Iフレーム）のタイムスタンプ（マイクロ秒）を取得する
     */
    private fun getKeyframeTimes(context: Context, uri: Uri): List<Long> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        // ビデオトラックのインデックスを探す
        var videoTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true) {
                videoTrackIndex = i
                break
            }
        }

        if (videoTrackIndex == -1) {
            extractor.release()
            return emptyList()
        }

        extractor.selectTrack(videoTrackIndex)
        val times = mutableListOf<Long>()

        // 最初（0秒時点）のキーフレームを取得して登録
        extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        var currentSampleTime = extractor.sampleTime

        if (currentSampleTime >= 0) {
            times.add(currentSampleTime)
        }

        // キーフレームからキーフレームへ直接ジャンプするループ
        while (currentSampleTime >= 0) {
            // 現在のタイムスタンプの「直後」のキーフレームへシーク
            extractor.seekTo(currentSampleTime + 1, MediaExtractor.SEEK_TO_NEXT_SYNC)
            val nextSampleTime = extractor.sampleTime

            // 同じ位置に戻ってしまった、または終端に達した場合はループを抜ける
            if (nextSampleTime < 0 || nextSampleTime <= currentSampleTime) {
                break
            }

            times.add(nextSampleTime)
            currentSampleTime = nextSampleTime
        }

        extractor.release()
        return times
    }

    /**
     * 指定されたタイムスタンプのリストを元に、MediaMetadataRetrieverを使ってBitmapを抽出する
     */
    private fun getKeyframeItems(
        context: Context,
        uri: Uri,
        timesUs: List<Long>,
        limit: Int
    ): List<KeyFrameResult> {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }

        val items = mutableListOf<KeyFrameResult>()

        // リミット件数分だけループを回してBitmapを生成
        for (timeUs in timesUs.take(limit)) {
            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )?.let { bitmap ->
                items.add(KeyFrameResult(timeUs, bitmap))
            }
        }

        retriever.release()
        return items
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        // 1. アプリ固有の内部ストレージのディレクトリを取得する
        //    filesDir の中に「images」という専用フォルダのパスを指定する
        val baseDirectory = context.filesDir
        val directory = File(baseDirectory, "images")

        // 2. 「images」フォルダが存在しない場合は、ここで自動作成する
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = UUID.randomUUID().toString()

        // 3. 保存するファイルを作成する
        val file = File(directory, fileName)

        var fileOutputStream: FileOutputStream? = null
        return try {
            // 4. 出力ストリームを開いてBitmapを圧縮・書き込みする
            fileOutputStream = FileOutputStream(file)

            // PNG形式（劣化なし）で保存。JPEGの場合は Bitmap.CompressFormat.JPEG
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)

            // 5. 保存に成功したら、絶対パスを文字列として返す
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null // 失敗した場合はnullを返す
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun addMicrosecondsToLong(
        baseTimeText: String,
        microsecondsToAdd: Long,
        pattern: String = "yyyy/MM/dd HH:mm:ss.SSS"
    ): Long {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern)

            // 1. 文字列を LocalDateTime にパース
            val localDateTime = LocalDateTime.parse(baseTimeText, formatter)

            // 2. マイクロ秒を加算
            val calculatedDateTime = localDateTime.plus(microsecondsToAdd, ChronoUnit.MICROS)

            // 3. タイムゾーン（端末のデフォルト）を考慮して Long 型（ミリ秒）に変換
            val zoneId = ZoneId.systemDefault()
            calculatedDateTime.atZone(zoneId).toInstant().toEpochMilli()

        } catch (e: Exception) {
            e.printStackTrace()
            0L // エラー時のフォールバック
        }
    }
}