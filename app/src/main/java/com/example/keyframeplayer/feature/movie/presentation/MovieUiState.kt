package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor

enum class TimeScale(
    val label: String,
    val totalSeconds: Float,
    val intervalMinutes: Int
) {
    ONE_DAY("1日", 86400f, 16),
    SIX_HOURS("6時間", 21600f, 4),
    ONE_HALF_HOURS("1.5時間", 5400f, 1);

    val intervalSeconds: Float
        get() = intervalMinutes * 60f

    val barCount: Int
        get() = ((totalSeconds / 60f) / intervalMinutes).toInt() // 90
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
    val seekTarget: SeekTarget? = null // ユーザー操作によるシーク要求
)

data class SeekTarget(
    val mediaItemIndex: Int,
    val positionMs: Long,
    val id: Long = System.currentTimeMillis()
)
