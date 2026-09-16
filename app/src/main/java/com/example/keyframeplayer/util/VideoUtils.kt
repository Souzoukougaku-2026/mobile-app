package com.example.keyframeplayer.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.DocumentsContract

// 画面やロジックで扱うための共通データ構造
data class VideoInfo(
    val uri: Uri,
    val name: String,
    val durationText: String,
    val startTimeText: String,
    val endTimeText: String,
)

/**
 * 動画ファイルの検索・アクセス権チェック・メタデータ解析に関する共通ロジックをまとめたユーティリティ
 */
object VideoUtils {

    /**
     * 💡 【ViewModelから移行】指定されたフォルダUriの読み込み権限があるか確認します。
     */
    fun checkAccess(context: Context, uri: Uri?): Boolean {
        /*val directory = uri?.let { DocumentFile.fromTreeUri(context, it) }
        return directory?.canRead() == true*/
        if (uri == null) return false
        return try {
            // Tree URI（フォルダ）形式であることを確認してからアクセスを試みる
            if (DocumentsContract.isTreeUri(uri)) {
                val directory = DocumentFile.fromTreeUri(context, uri)
                directory?.canRead() == true
            } else {
                false
            }
        } catch (e: Exception) {
            // 万が一不正なURIで例外が発生してもアプリを落とさない
            false
        }
    }

    /**
     * 💡 指定されたフォルダUri内からmp4ファイルを検索し、解析済みのVideoInfoリストとして返します。
     */
    fun fetchVideoInfosFromDirectory(context: Context, directoryUri: Uri): List<VideoInfo> {
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return emptyList()

        // フォルダ内のファイルから「.mp4」かつ「ファイルであるもの」だけを抽出
        val mp4Files = directory.listFiles().filter {
            it.isFile && it.name?.lowercase()?.endsWith(".mp4") == true
        }

        // 抽出したDocumentFileのリストを解析して VideoInfo に変換
        return loadVideoInfos(context, mp4Files)
    }

    /**
     * DocumentFileのリストを解析し、範囲選択に必要な動画情報リストに変換する（内部処理用）
     */
    private fun loadVideoInfos(context: Context, mp4Files: List<DocumentFile>): List<VideoInfo> {
        val retriever = MediaMetadataRetriever()
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())

        return mp4Files.map { file ->
            var durationMs = 0L
            try {
                retriever.setDataSource(context, file.uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val endTimeMs: Long = file.lastModified()
            val startTimeMs: Long = endTimeMs - durationMs

            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val durationText = String.format("%02d:%02d", minutes, seconds)

            VideoInfo(
                uri = file.uri,
                name = file.name ?: "Unknown",
                durationText = durationText,
                startTimeText = formatter.format(Date(startTimeMs)),
                endTimeText = formatter.format(Date(endTimeMs))
            )
        }.also {
            retriever.release() // 最後に必ずリソースを解放
        }
    }

    /**
     * 動画ファイルのUriから、直接再生時間を取得するシンプルな関数
     */
    fun getVideoDuration(context: Context, fileUri: Uri): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, fileUri)
            val timeInMilliSec = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val totalSeconds = timeInMilliSec / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format("%02d:%02d", minutes, seconds)
        } catch (e: Exception) {
            e.printStackTrace()
            "00:00"
        } finally {
            retriever.release()
        }
    }
}
