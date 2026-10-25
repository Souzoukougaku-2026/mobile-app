package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import java.util.TimeZone

/**
 * 表示スケールの定義
 * @param label 画面上の表記
 * @param windowSeconds グラフ横幅全体 (1画面表示窓) の時間帯 (秒)
 * @param majorTickSeconds 大目盛り（数字ラベル付き主目盛り）の間隔 (秒)
 * @param minorTickSeconds 小目盛り（副目盛り線）の間隔 (秒)
 */
enum class TimeScale(
    val label: String,
    val windowSeconds: Long,
    val majorTickSeconds: Long,
    val minorTickSeconds: Long
) {
    THREE_DAYS(
        label = "3日",
        windowSeconds = 3 * 24 * 3600L, // 72時間
        majorTickSeconds = 24 * 3600L,  // 12時間
        minorTickSeconds = 6 * 3600L    // 2時間
    ),
    ONE_DAY(
        label = "1日",
        windowSeconds = 24 * 3600L,     // 24時間
        majorTickSeconds = 6 * 3600L,   // 6時間 (00:00, 06:00, 12:00, 18:00)
        minorTickSeconds = 1 * 3600L    // 1時間
    ),
    SIX_HOURS(
        label = "6時間",
        windowSeconds = 6 * 3600L,      // 6時間
        majorTickSeconds = 1 * 3600L,   // 1時間 (12:00, 13:00, 14:00...)
        minorTickSeconds = 10 * 60L     // 10分
    ),
    THREE_HOURS(
        label = "3時間",
        windowSeconds = 3 * 3600L,      // 3時間
        majorTickSeconds = 1 * 3600L,   // 1時間 (12:00, 13:00, 14:00, 15:00)
        minorTickSeconds = 10 * 60L     // 10分 (1時間を6等分)
    ),
    ONE_HALF_HOURS(
        label = "1.5時間",
        windowSeconds = 5400L,          // 1.5時間 (90分)
        majorTickSeconds = 30 * 60L,    // 30分 (12:00, 12:30, 13:00...)
        minorTickSeconds = 5 * 60L      // 5分
    ),
    THIRTY_MINUTES(
        label = "30分",
        windowSeconds = 1800L,          // 30分
        majorTickSeconds = 10 * 60L,    // 10分 (12:00, 12:10, 12:20...)
        minorTickSeconds = 2 * 60L      // 1分
    ),
    TEN_MINUTES(
        label = "10分",
        windowSeconds = 600L,           // 10分
        majorTickSeconds = 2 * 60L,     // 2分 (12:00, 12:02, 12:04...)
        minorTickSeconds = 30L          // 30秒
    ),
    FIVE_MINUTES(
        label = "5分",
        windowSeconds = 300L,           // 5分
        majorTickSeconds = 60L,         // 1分 (12:00, 12:01, 12:02...)
        minorTickSeconds = 10L          // 10秒
    ),
    ONE_MINUTE(
        label = "1分",
        windowSeconds = 60L,            // 1分
        majorTickSeconds = 10L,         // 10秒 (12:00:00, 12:00:10...)
        minorTickSeconds = 2L           // 2秒
    );

    val windowMs: Long get() = windowSeconds * 1000L
    val majorTickMs: Long get() = majorTickSeconds * 1000L
    val minorTickMs: Long get() = minorTickSeconds * 1000L

    val totalSeconds: Float get() = windowSeconds.toFloat()
    val intervalMinutes: Float get() = majorTickSeconds / 60f

    val barCountPerWindow: Int = 90
    val barCount: Int get() = barCountPerWindow

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

private fun alignToRoundTime(timeMs: Long, intervalMs: Long): Long {
    if (intervalMs <= 0L) return timeMs
    val tzOffset = TimeZone.getDefault().getOffset(timeMs)
    val localMs = timeMs + tzOffset
    val alignedLocalMs = (localMs / intervalMs) * intervalMs
    return alignedLocalMs - tzOffset
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
    val isLikedFilterOnly: Boolean = false, // true の場合「いいね」された画像のみ表示
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
            val scaleWindowMs = selectedTimeScale.windowMs
            val spanMs = maxOf(totalRealDurationMs, scaleWindowMs)
            return (scaleWindowMs.toFloat() / spanMs.toFloat()).coerceIn(0.05f, 1f)
        }

    /**
     * 選択されている TimeScale の正時目盛り刻みに完全に合致した【アライン済みの開始絶対時刻(ms)】と【総時間幅(ms)】を取得する
     */
    fun getAlignedBounds(): Pair<Long, Long> {
        val intervalMs = selectedTimeScale.majorTickMs
        val scaleWindowMs = selectedTimeScale.windowMs

        val rawMinReal = minRealTimeMs
        val rawSpanMs = maxOf(totalRealDurationMs, scaleWindowMs)
        val rawMaxReal = rawMinReal + rawSpanMs

        val minReal = alignToRoundTime(rawMinReal, intervalMs)
        var alignedMaxReal = alignToRoundTime(rawMaxReal, intervalMs)
        if (alignedMaxReal < minReal + scaleWindowMs) {
            alignedMaxReal = minReal + scaleWindowMs
        }
        val spanMs = alignedMaxReal - minReal
        return minReal to spanMs
    }
}

data class SeekTarget(
    val mediaItemIndex: Int,
    val positionMs: Long,
    val id: Long = System.currentTimeMillis()
)
