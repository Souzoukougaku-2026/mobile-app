package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor

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
        majorTickSeconds = 12 * 3600L,  // 18時間
        minorTickSeconds = 3 * 3600L    // 3時間
    ),
    ONE_DAY(
        label = "1日",
        windowSeconds = 24 * 3600L,     // 24時間 (86400秒)
        majorTickSeconds = 6 * 3600L,   // 6時間 (00:00, 06:00, 12:00, 18:00)
        minorTickSeconds = 1 * 3600L    // 1時間
    ),
    TWELVE_HOURS(
        label = "12時間",
        windowSeconds = 12 * 3600L,     // 12時間 (43200秒)
        majorTickSeconds = 3 * 3600L,   // 3時間 (00:00, 03:00, 06:00, 09:00, 12:00...)
        minorTickSeconds = 30 * 60L     // 30分
    ),
    SIX_HOURS(
        label = "6時間",
        windowSeconds = 6 * 3600L,      // 6時間 (21600秒)
        majorTickSeconds = 1 * 3600L,   // 1時間 (12:00, 13:00, 14:00)
        minorTickSeconds = 10 * 60L     // 10分
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
    val totalRealDurationMs: Long
        get() {
            val diff = maxRealTimeMs - minRealTimeMs
            return if (diff in 1000L..(30L * 24 * 3600 * 1000L)) {
                diff
            } else {
                (totalDurationSeconds * 1000f).toLong().coerceAtLeast(1000L)
            }
        }
}

data class SeekTarget(
    val mediaItemIndex: Int,
    val positionMs: Long,
    val id: Long = System.currentTimeMillis()
)
