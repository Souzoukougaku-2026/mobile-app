package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import java.util.TimeZone

/**
 * 画面上のビューポート（描画可能範囲の横幅全体）がカバーする時間情報
 */
data class ViewportTimeRange(
    val startRealTimeMs: Long,
    val endRealTimeMs: Long,
    val durationMs: Long // ＝ 描画可能範囲の横幅の時間差
)

/**
 * 表示スケールの定義
 * @param label 画面上の表記
 * @param windowSeconds 1画面（ビューポート）に収める基準時間（秒）
 * @param majorTickSeconds 大目盛り（数字ラベル付き主目盛り）の間隔（秒）
 * @param minorTickSeconds 小目盛り（副目盛り線）の間隔（秒）
 */
enum class TimeScale(
    val label: String,
    val windowSeconds: Long,
    val majorTickSeconds: Long,
    val minorTickSeconds: Long
) {
    THREE_DAYS(
        label = "3日",
        windowSeconds = 3 * 24 * 3600L, // 72時間 (259200秒)
        majorTickSeconds = 18 * 3600L,  // 18時間
        minorTickSeconds = 3 * 3600L    // 3時間
    ),
    ONE_DAY(
        label = "1日",
        windowSeconds = 24 * 3600L,     // 24時間 (86400秒)
        majorTickSeconds = 6 * 3600L,   // 6時間 (00:00, 06:00, 12:00, 18:00)
        minorTickSeconds = 1 * 3600L    // 1時間
    ),
    SIX_HOURS(
        label = "6時間",
        windowSeconds = 6 * 3600L,      // 6時間 (21600秒)
        majorTickSeconds = 3600L,       // 1時間 (12:00, 13:00, 14:00)
        minorTickSeconds = 600L         // 10分
    ),
    THREE_HOURS(
        label = "3時間",
        windowSeconds = 3 * 3600L,      // 3時間 (10800秒)
        majorTickSeconds = 3600L,       // 1時間 (12:00, 13:00, 14:00, 15:00)
        minorTickSeconds = 600L         // 10分
    ),
    ONE_HALF_HOURS(
        label = "1.5時間",
        windowSeconds = 5400L,          // 1.5時間 (90分 = 5400秒)
        majorTickSeconds = 1800L,       // 30分 (12:00, 12:30, 13:00)
        minorTickSeconds = 300L         // 5分
    ),
    THIRTY_MINUTES(
        label = "30分",
        windowSeconds = 1800L,          // 30分 (1800秒)
        majorTickSeconds = 600L,        // 10分 (12:00, 12:10, 12:20)
        minorTickSeconds = 60L          // 1分
    ),
    TEN_MINUTES(
        label = "10分",
        windowSeconds = 600L,           // 10分 (600秒)
        majorTickSeconds = 120L,        // 2分 (12:00, 12:02, 12:04)
        minorTickSeconds = 25L          // 25秒
    ),
    FIVE_MINUTES(
        label = "5分",
        windowSeconds = 300L,           // 5分 (300秒)
        majorTickSeconds = 60L,         // 1分 (12:00, 12:01, 12:02)
        minorTickSeconds = 12L          // 12秒
    ),
    ONE_MINUTE(
        label = "1分",
        windowSeconds = 60L,            // 1分 (60秒)
        majorTickSeconds = 10L,         // 10秒 (12:00:00, 12:00:10)
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
    val videoOffsetsMs: List<Long> = emptyList(),
    val keyframes: List<CropImage> = emptyList(),

    val currentTime: Float = 0f,
    val totalDurationSeconds: Float = 3600f,

    // 時間スケール・フィルター状態
    val selectedTimeScale: TimeScale = TimeScale.SIX_HOURS,
    val selectedClassFilter: String? = null,
    val selectedColorFilter: ImageColor? = null,
    val isLikedFilterOnly: Boolean = false,
    val availableClasses: List<String> = emptyList(),
    val availableColors: List<ImageColor> = ImageColor.entries,

    val graphBarCounts: List<Int> = emptyList(),
    val maxBarCount: Int = 1,

    val isLoading: Boolean = false,
    val error: String? = null,

    val initialMediaItemIndex: Int = 0,
    val initialPositionMs: Long = 0L,
    val currentRealTimeMs: Long = 0L,
    val minRealTimeMs: Long = 0L,
    val maxRealTimeMs: Long = 0L,
    val seekTarget: SeekTarget? = null
) {
    /**
     * 現在選択されているグラフ1画面（描画可能範囲の横幅全体）がカバーする時間差（ミリ秒）
     */
    val scaleWindowMs: Long
        get() = selectedTimeScale.windowMs

    /**
     * 現在選択されているグラフ1画面（描画可能範囲の横幅全体）がカバーする時間差（秒）
     */
    val scaleWindowSeconds: Long
        get() = selectedTimeScale.windowSeconds

    val totalRealDurationMs: Long
        get() {
            val diff = maxRealTimeMs - minRealTimeMs
            return if (diff in 1000L..(30L * 24 * 3600 * 1000L)) {
                diff
            } else {
                (totalDurationSeconds * 1000f).toLong().coerceAtLeast(1000L)
            }
        }

    val visibleRangeWidth: Float
        get() {
            val windowMs = selectedTimeScale.windowMs
            val spanMs = maxOf(totalRealDurationMs, windowMs)
            return (windowMs.toFloat() / spanMs.toFloat()).coerceIn(0.05f, 1f)
        }

    /**
     * 画面上のスクロール状態（ピクセル位置）から、現在画面に映っている【描画可能範囲の横幅の時間差】を明示的に算出・取得する
     */
    fun getViewportTimeRange(
        scrollOffsetPx: Float,
        viewportWidthPx: Float,
        canvasTotalWidthPx: Float
    ): ViewportTimeRange {
        val windowMs = selectedTimeScale.windowMs
        val totalSpanMs = maxOf(totalRealDurationMs, windowMs)

        if (canvasTotalWidthPx <= 0f || totalSpanMs <= 0L) {
            return ViewportTimeRange(
                startRealTimeMs = minRealTimeMs,
                endRealTimeMs = minRealTimeMs + windowMs,
                durationMs = windowMs
            )
        }

        val startMs = minRealTimeMs + ((scrollOffsetPx / canvasTotalWidthPx) * totalSpanMs).toLong()
        val endMs = minRealTimeMs + (((scrollOffsetPx + viewportWidthPx) / canvasTotalWidthPx) * totalSpanMs).toLong()
        val timeDiffMs = endMs - startMs

        return ViewportTimeRange(
            startRealTimeMs = startMs,
            endRealTimeMs = endMs,
            durationMs = timeDiffMs
        )
    }

    /**
     * 選択されている TimeScale の正時目盛り刻みに完全に合致した【アライン済みの開始絶対時刻(ms)】と【総時間幅(ms)】を取得する
     */
    fun getAlignedBounds(): Pair<Long, Long> {
        val intervalMs = selectedTimeScale.majorTickMs
        val windowMs = selectedTimeScale.windowMs

        val rawMinReal = minRealTimeMs
        val rawSpanMs = maxOf(totalRealDurationMs, windowMs)
        val rawMaxReal = rawMinReal + rawSpanMs

        val minReal = alignToRoundTime(rawMinReal, intervalMs)
        var alignedMaxReal = alignToRoundTime(rawMaxReal, intervalMs)
        if (alignedMaxReal < minReal + windowMs) {
            alignedMaxReal = minReal + windowMs
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
