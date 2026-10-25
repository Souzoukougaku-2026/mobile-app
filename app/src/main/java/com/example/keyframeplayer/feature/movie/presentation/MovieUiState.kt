package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import java.util.TimeZone

/**
 * 新設計: タイムスケール定義
 * @param label スケール名
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
    THREE_DAYS(label = "3日", windowSeconds = 3 * 24 * 3600L, majorTickSeconds = 18 * 3600L, minorTickSeconds = 3 * 3600L),
    ONE_DAY(label = "1日", windowSeconds = 24 * 3600L, majorTickSeconds = 6 * 3600L, minorTickSeconds = 1 * 3600L),
    SIX_HOURS(label = "6時間", windowSeconds = 6 * 3600L, majorTickSeconds = 1 * 3600L, minorTickSeconds = 10 * 60L),
    THREE_HOURS(label = "3時間", windowSeconds = 3 * 3600L, majorTickSeconds = 1 * 3600L, minorTickSeconds = 10 * 60L),
    ONE_HALF_HOURS(label = "1.5時間", windowSeconds = 5400L, majorTickSeconds = 30 * 60L, minorTickSeconds = 5 * 60L),
    THIRTY_MINUTES(label = "30分", windowSeconds = 1800L, majorTickSeconds = 10 * 60L, minorTickSeconds = 1 * 60L),
    TEN_MINUTES(label = "10分", windowSeconds = 600L, majorTickSeconds = 2 * 60L, minorTickSeconds = 30L),
    FIVE_MINUTES(label = "5分", windowSeconds = 300L, majorTickSeconds = 60L, minorTickSeconds = 10L),
    ONE_MINUTE(label = "1分", windowSeconds = 60L, majorTickSeconds = 10L, minorTickSeconds = 2L);

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
