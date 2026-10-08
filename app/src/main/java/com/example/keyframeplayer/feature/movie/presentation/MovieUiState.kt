package com.example.keyframeplayer.feature.movie.presentation

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.domain.model.CropImage

@Immutable
data class MovieUiState(
    val videoUri: android.net.Uri? = null,
    val videos: List<VideoEntity> = emptyList(),
    val videoOffsetsMs: List<Long> = emptyList(), // 各動画の累積開始ミリ秒 (0, D0, D0+D1, ...)
    val keyframes: List<CropImage> = emptyList(),

    val currentTime: Float = 0f, // 全体時間軸における現在の再生時間 (秒)
    val totalDurationSeconds: Float = 3600f, // 全体総再生時間 (秒)
    val viewportDurationSeconds: Float = 600f, // 詳細グラフ表示領域 (秒)

    val overallBarCount: Int = 144,
    val detailedBarCountPerHour: Int = 120,

    val barValues: List<Float> = emptyList(),
    val detailedBarValues: List<Float> = emptyList(),
    val visibleRangeStart: Float = 0f, // 0.0 to 1.0
    val isLoading: Boolean = false,
    val error: String? = null,

    val initialMediaItemIndex: Int = 0,
    val initialPositionMs: Long = 0L,
    val currentRealTimeMs: Long = 0L, // 現実絶対時刻 (Epoch ms)
    val seekTarget: SeekTarget? = null // ユーザー操作によるシーク要求
) {
    val visibleRangeWidth: Float
        get() = if (totalDurationSeconds > 0) (viewportDurationSeconds / totalDurationSeconds).coerceIn(0f, 1f) else 1f

    val totalDetailedBarCount: Int
        get() = (detailedBarCountPerHour * (totalDurationSeconds / 3600f)).toInt().coerceAtLeast(1)
}

data class SeekTarget(
    val mediaItemIndex: Int,
    val positionMs: Long,
    val id: Long = System.currentTimeMillis()
)
