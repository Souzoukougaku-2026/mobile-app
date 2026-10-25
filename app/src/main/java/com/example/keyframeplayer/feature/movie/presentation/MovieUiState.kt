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
        windowSeconds = 3 * 24 * 3600L, // 72時間 (259200秒)
        majorTickSeconds = 18 * 3600L,  // 18時間 (72h / 4)
        minorTickSeconds = 3 * 3600L    // 3時間 (72h / 24)
    ),
    ONE_DAY(
        label = "1日",
        windowSeconds = 24 * 3600L,     // 24時間 (86400秒)
        majorTickSeconds = 6 * 3600L,   // 6時間 (24h / 4) [★1日表示の基準スタイル]
        minorTickSeconds = 1 * 3600L    // 1時間 (24h / 24)
    ),
    SIX_HOURS(
        label = "6時間",
        windowSeconds = 6 * 3600L,      // 6時間 (21600秒)
        majorTickSeconds = 5400L,       // 1.5時間 (90分 = 6h / 4)
        minorTickSeconds = 900L         // 15分 (6h / 24)
    ),
    THREE_HOURS(
        label = "3時間",
        windowSeconds = 3 * 3600L,      // 3時間 (10800秒)
        majorTickSeconds = 2700L,       // 45分 (3h / 4)
        minorTickSeconds = 450L         // 7.5分 (7分30秒 = 3h / 24)
    ),
    ONE_HALF_HOURS(
        label = "1.5時間",
        windowSeconds = 5400L,          // 1.5時間 (90分 = 5400秒)
        majorTickSeconds = 1350L,       // 22.5分 (22分30秒 = 1.5h / 4)
        minorTickSeconds = 225L         // 3.75分 (3分45秒 = 1.5h / 24)
    ),
    THIRTY_MINUTES(
        label = "30分",
        windowSeconds = 1800L,          // 30分 (1800秒)
        majorTickSeconds = 450L,        // 7.5分 (7分30秒 = 30m / 4)
        minorTickSeconds = 75L          // 1.25分 (1分15秒 = 30m / 24)
    ),
    TEN_MINUTES(
        label = "10分",
        windowSeconds = 600L,           // 10分 (600秒)
        majorTickSeconds = 150L,        // 2.5分 (2分30秒 = 10m / 4)
        minorTickSeconds = 25L          // 25秒 (10m / 24)
    ),
    FIVE_MINUTES(
        label = "5分",
        windowSeconds = 300L,           // 5分 (300秒)
        majorTickSeconds = 75L,         // 1.25分 (1分15秒 = 5m / 4)
        minorTickSeconds = 12L          // 12.5秒 (5m / 24)
    ),
    ONE_MINUTE(
        label = "1分",
        windowSeconds = 60L,            // 1分 (60秒)
        majorTickSeconds = 15L,         // 15秒 (1m / 4)
        minorTickSeconds = 2L           // 2.5秒 (1m / 24)
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
