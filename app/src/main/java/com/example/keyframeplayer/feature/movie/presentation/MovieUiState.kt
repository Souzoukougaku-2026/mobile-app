package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor

enum class TimeScale(
    val label: String,
    val totalSeconds: Float,
    val intervalMinutes: Float
) {
    THREE_DAYS("3日", 259200f, 48f),
    ONE_DAY("1日", 86400f, 16f),
    SIX_HOURS("6時間", 21600f, 4f),
    THREE_HOURS("3時間", 10800f, 2f),
    ONE_HALF_HOURS("1.5時間", 5400f, 1f);

    val intervalSeconds: Float
        get() = intervalMinutes * 60f

    val barCountPerWindow: Int = 90

    val barCount: Int
        get() = barCountPerWindow

    fun zoomIn(): TimeScale {
        val entries = TimeScale.entries
        val idx = entries.indexOf(this)
        return if (idx < entries.lastIndex) entries[idx + 1] else this
    }

    fun zoomOut(): TimeScale {
        val entries = TimeScale.entries
        val idx = entries.indexOf(this)
        return if (idx > 0) entries[idx - 1] else this
    }
}

@Immutable
data class MovieUiState(
    val videoUri: android.net.Uri? = null,
    val videos: List<VideoEntity> = emptyList(),
    val videoOffsetsMs: List<Long> = emptyList(), // 各動画の累積開始ミリ秒 (0, D0, D0+D1, ...)
    val keyframes: List<CropImage> = emptyList(),

    val currentTime: Float = 0f, // 全体時間軸における現在の再生時間 (秒)
    val totalDurationSeconds: Float = 3600f, // 全体総再生時間 (秒)

    // 時間スケール・フィルター状態
    val selectedTimeScale: TimeScale = TimeScale.SIX_HOURS,
    val selectedClassFilter: String? = null, // null は「すべて」
    val selectedColorFilter: ImageColor? = null, // null は「すべて」
    val availableClasses: List<String> = emptyList(),
    val availableColors: List<ImageColor> = ImageColor.entries,

    // 統合グラフのバー別件数データ (要素数 = selectedTimeScale.barCount)
    val graphBarCounts: List<Int> = emptyList(),
    val maxBarCount: Int = 1,

    val isLoading: Boolean = false,
    val error: String? = null,

    val initialMediaItemIndex: Int = 0,
    val initialPositionMs: Long = 0L,
    val currentRealTimeMs: Long = 0L, // 現実絶対時刻 (Epoch ms)
    val minRealTimeMs: Long = 0L,     // 動画ファイル群/キーフレームの最小現実時刻 (Epoch ms)
    val maxRealTimeMs: Long = 0L,     // 動画ファイル群/キーフレームの最大現実時刻 (Epoch ms)
    val seekTarget: SeekTarget? = null // ユーザー操作によるシーク要求
) {
    val totalRealDurationMs: Long
        get() {
            val diff = maxRealTimeMs - minRealTimeMs
            // 1秒 〜 30日間の正常範囲にガード。異常値（Epoch msの初期値等）の場合は動画合計再生時間にフォールバック
            return if (diff in 1000L..(30L * 24 * 3600 * 1000L)) {
                diff
            } else {
                (totalDurationSeconds * 1000f).toLong().coerceAtLeast(1000L)
            }
        }

    val visibleRangeWidth: Float
        get() {
            val spanMs = maxOf(totalRealDurationMs.toFloat(), 24 * 3600 * 1000f)
            val scaleWindowMs = selectedTimeScale.totalSeconds * 1000f
            return (scaleWindowMs / spanMs).coerceIn(0.05f, 1f)
        }
}

data class SeekTarget(
    val mediaItemIndex: Int,
    val positionMs: Long,
    val id: Long = System.currentTimeMillis()
)
