package com.example.a2026souzou.feature.movie.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class MovieUiState(
    val currentTime: Float = 0f,
    // 時間設定
    val totalDurationSeconds: Float = 48 * 3600f, // 全体時間 (48時間)
    val viewportDurationSeconds: Float = 1 * 3600f, // 詳細表示幅 (1時間)
    
    // バーの本数設定
    val overallBarCount: Int = 144, // 20分に1本
    val detailedBarCountPerHour: Int = 120, // 30秒に1本
    
    val barValues: List<Float> = emptyList(),
    val detailedBarValues: List<Float> = emptyList(),
    val visibleRangeStart: Float = 0f, // 0.0 to 1.0
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val visibleRangeWidth: Float 
        get() = viewportDurationSeconds / totalDurationSeconds

    val totalDetailedBarCount: Int
        get() = (detailedBarCountPerHour * (totalDurationSeconds / 3600f)).toInt()
}
