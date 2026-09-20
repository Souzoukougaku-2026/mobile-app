package com.example.keyframeplayer.ui.movie

import androidx.compose.runtime.Immutable
import com.example.keyframeplayer.model.CropImage
import com.example.keyframeplayer.util.VideoInfo

@Immutable
data class MovieUiState(
    val selectedVideos: List<VideoInfo> = emptyList(),
    val sessionStartTimeMs: Long = 0L,
    val videoUri: android.net.Uri? = null,
    val keyframes: List<CropImage> = emptyList(),
    val selectedThumbnailPath: String? = null,
    val selectedBBox: android.graphics.RectF? = null,
    
    // グラフフィルタ設定
    val filterClass: String? = null,
    val filterColor: Int? = null,

    val currentTime: Float = 0f,
    // 時間設定
    val totalDurationSeconds: Float = 48 * 3600f,
    val overallViewportSeconds: Float = 6 * 3600f,  // 全体グラフ表示幅 (6h)
    val detailedViewportSeconds: Float = 1 * 3600f, // 詳細グラフ表示幅 (1h)
    
    // バーの本数設定
    val overallBarCountPerHour: Int = 3,
    val detailedBarCountPerHour: Int = 120,
    
    val barValues: List<Float> = emptyList(),
    val detailedBarValues: List<Float> = emptyList(),
    
    val overallScrollStart: Float = 0f,
    val detailedScrollStart: Float = 0f,
    
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // 画面幅に対して、データ全体が占めるべき倍率
    val overallContentScale: Float 
        get() = totalDurationSeconds / overallViewportSeconds

    val detailedContentScale: Float 
        get() = totalDurationSeconds / detailedViewportSeconds

    // 画面幅を 1.0 としたときのビューポートの比率
    val overallViewportWidth: Float
        get() = (overallViewportSeconds / totalDurationSeconds).coerceAtMost(1f)

    val detailedViewportWidth: Float
        get() = (detailedViewportSeconds / totalDurationSeconds).coerceAtMost(1f)

    // 全体グラフ上の詳細枠（1時間）の幅比率
    val detailHighlightWidthRatio: Float
        get() = detailedViewportSeconds / totalDurationSeconds

    val totalOverallBarCount: Int
        get() = (overallBarCountPerHour * (totalDurationSeconds / 3600f)).toInt()

    val totalDetailedBarCount: Int
        get() = (detailedBarCountPerHour * (totalDurationSeconds / 3600f)).toInt()
}
