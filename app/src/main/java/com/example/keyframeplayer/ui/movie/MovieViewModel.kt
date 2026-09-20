package com.example.keyframeplayer.ui.movie

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.data.AppDatabase
import com.example.keyframeplayer.model.CropImage
import com.example.keyframeplayer.model.ImageColor
import com.example.keyframeplayer.util.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovieViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private var isSeeking = false

    private val database = AppDatabase.getDatabase(application)
    private val clopImageDao = database.clopImageDao()
    private val keyFrameDao = database.keyFrameDao()

    init {
        fetchBarData()
        fetchDetailedBarData()
        fetchKeyframes()
    }

    fun setVideoSession(videos: List<VideoInfo>) {
        if (videos.isEmpty()) return

        val sortedVideos = videos.sortedBy { it.startTimeMs }
        val startMs = sortedVideos.first().startTimeMs
        val endMs = sortedVideos.last().endTimeMs
        val totalSec = (endMs - startMs) / 1000f

        _uiState.update { 
            it.copy(
                selectedVideos = sortedVideos,
                sessionStartTimeMs = startMs,
                totalDurationSeconds = totalSec,
                currentTime = 0f
            )
        }
        
        onVideoSelected(sortedVideos.first().uri)
        
        // セッションが確定したので、その動画群に紐づく物体データを取得
        fetchKeyframes()
    }

    fun setSelectedThumbnail(path: String?, bbox: android.graphics.RectF? = null) {
        _uiState.update { it.copy(
            selectedThumbnailPath = path,
            selectedBBox = bbox
        ) }
    }

    private fun fetchKeyframes() {
        val state = _uiState.value
        if (state.selectedVideos.isEmpty()) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // セッションに含まれる全動画のURIを取得
                val uris = state.selectedVideos.map { it.uri }
                
                // JOINクエリを使用して、現在再生中の動画セッションに属する物体データのみを取得
                val objectData = clopImageDao.getObjectsForVideos(uris)
                
                val keyframes = objectData.map { item ->
                    CropImage(
                        id = item.clop.uuid,
                        className = item.clop.classname,
                        score = item.clop.score.toDouble(),
                        color = ImageColor.fromId(item.clop.color.ordinal),
                        bboxLeft = item.clop.bboxPoint.ulX.toInt(),
                        bboxTop = item.clop.bboxPoint.ulY.toInt(),
                        bboxRight = item.clop.bboxPoint.lrX.toInt(),
                        bboxBottom = item.clop.bboxPoint.lrY.toInt(),
                        realTime = item.realTime,
                        idKeyFrame = item.clop.idKeyFrame
                    )
                }
                _uiState.update { it.copy(keyframes = keyframes) }
                
                // 物体密度に基づいてグラフを再描画
                fetchBarData()
                fetchDetailedBarData()
            }
        }
    }

    private fun fetchBarData() {
        val totalSec = _uiState.value.totalDurationSeconds
        if (totalSec <= 0) return
        
        val barCount = _uiState.value.totalOverallBarCount
        if (barCount <= 0) return

        val keyframes = getFilteredKeyframes() // フィルタを適用
        val sessionStart = _uiState.value.sessionStartTimeMs
        
        // 各バーの時間枠（秒）
        val secPerBar = totalSec / barCount
        val counts = IntArray(barCount) { 0 }

        // 時間枠ごとにカウント
        keyframes.forEach { kf ->
            val relativeSec = (kf.realTime - sessionStart) / 1000f
            val index = (relativeSec / secPerBar).toInt().coerceIn(0, barCount - 1)
            counts[index]++
        }

        // 高さに変換 (5個以上でMAX)
        val maxThreshold = 5f
        val values = counts.map { (it.toFloat() / maxThreshold).coerceAtMost(1.0f) }
        
        _uiState.update { it.copy(barValues = values) }
    }

    private fun fetchDetailedBarData() {
        val totalSec = _uiState.value.totalDurationSeconds
        if (totalSec <= 0) return

        val barCount = _uiState.value.totalDetailedBarCount
        if (barCount <= 0) return

        val keyframes = getFilteredKeyframes() // フィルタを適用
        val sessionStart = _uiState.value.sessionStartTimeMs
        
        val secPerBar = totalSec / barCount
        val counts = IntArray(barCount) { 0 }

        keyframes.forEach { kf ->
            val relativeSec = (kf.realTime - sessionStart) / 1000f
            val index = (relativeSec / secPerBar).toInt().coerceIn(0, barCount - 1)
            counts[index]++
        }

        // 高さに変換 (1個でもあれば目立たせたいので、少し下駄を履かせる)
        val maxThreshold = 3f
        val values = counts.map { 
            if (it > 0) (it.toFloat() / maxThreshold).coerceIn(0.2f, 1.0f) else 0f 
        }
        
        _uiState.update { it.copy(detailedBarValues = values) }
    }

    fun onVideoSelected(uri: Uri) {
        _uiState.update { it.copy(videoUri = uri) }
    }

    fun onSeekStarted() {
        isSeeking = true
    }

    fun onTimeChanged(newTime: Float, seekPlayer: Boolean = false) {
        val state = _uiState.value
        if (state.selectedVideos.isEmpty()) {
            _uiState.update { it.copy(currentTime = newTime.coerceIn(0f, it.totalDurationSeconds)) }
            return
        }

        val oldTime = state.currentTime
        val clampedTime = newTime.coerceIn(0f, state.totalDurationSeconds)
        val absoluteRequestedMs = state.sessionStartTimeMs + (clampedTime * 1000).toLong()

        val targetVideoIndex = state.selectedVideos.indexOfFirst { 
            absoluteRequestedMs >= it.startTimeMs && absoluteRequestedMs <= it.endTimeMs 
        }

        if (targetVideoIndex != -1) {
            val targetVideo = state.selectedVideos[targetVideoIndex]
            val relativePosMs = absoluteRequestedMs - targetVideo.startTimeMs
            
            _uiState.update { it.copy(currentTime = clampedTime, videoUri = targetVideo.uri) }
            if (seekPlayer) {
                _seekRequest.value = SeekEvent(targetVideoIndex, relativePosMs)
            }
        } else {
            if (clampedTime > oldTime) {
                val nextVideoIndex = state.selectedVideos.indexOfFirst { it.startTimeMs > absoluteRequestedMs }
                if (nextVideoIndex != -1) {
                    val nextVideo = state.selectedVideos[nextVideoIndex]
                    val skipToTime = (nextVideo.startTimeMs - state.sessionStartTimeMs) / 1000f
                    _uiState.update { it.copy(currentTime = skipToTime, videoUri = nextVideo.uri) }
                    if (seekPlayer) _seekRequest.value = SeekEvent(nextVideoIndex, 0L)
                }
            } else {
                val prevVideoIndex = state.selectedVideos.indexOfLast { it.endTimeMs < absoluteRequestedMs }
                if (prevVideoIndex != -1) {
                    val prevVideo = state.selectedVideos[prevVideoIndex]
                    val skipToTime = (prevVideo.endTimeMs - state.sessionStartTimeMs) / 1000f
                    _uiState.update { it.copy(currentTime = skipToTime, videoUri = prevVideo.uri) }
                    if (seekPlayer) {
                        val durationMs = prevVideo.endTimeMs - prevVideo.startTimeMs
                        _seekRequest.value = SeekEvent(prevVideoIndex, durationMs)
                    }
                }
            }
        }
    }

    fun onSeekFinished(finalTime: Float) {
        onTimeChanged(finalTime, seekPlayer = true)
        isSeeking = false
    }

    fun skipTime(delta: Float) {
        onTimeChanged(_uiState.value.currentTime + delta, seekPlayer = true)
    }

    data class SeekEvent(val index: Int, val positionMs: Long)
    private val _seekRequest = MutableStateFlow<SeekEvent?>(null)
    val seekRequest: StateFlow<SeekEvent?> = _seekRequest.asStateFlow()

    fun onSeekProcessed() {
        _seekRequest.value = null
    }

    fun onPlayerPositionChanged(mediaItemIndex: Int, positionMs: Long) {
        if (isSeeking) return

        val state = _uiState.value
        if (mediaItemIndex < state.selectedVideos.size) {
            val video = state.selectedVideos[mediaItemIndex]
            val absoluteMs = video.startTimeMs + positionMs
            val sessionSec = (absoluteMs - state.sessionStartTimeMs) / 1000f
            _uiState.update { it.copy(currentTime = sessionSec) }
        }
    }

    fun onProgressTapped(progress: Float) {
        val newTime = progress * _uiState.value.totalDurationSeconds
        onTimeChanged(newTime, seekPlayer = true)
    }

    fun onFilterChanged(className: String?, colorId: Int?) {
        _uiState.update { it.copy(filterClass = className, filterColor = colorId) }
        fetchBarData()
        fetchDetailedBarData()
    }
    
    private fun getFilteredKeyframes(): List<CropImage> {
        val state = _uiState.value
        return state.keyframes.filter { kf ->
            val matchClass = state.filterClass == null || kf.className == state.filterClass
            // Colorの比較は現状は簡易的に (TODO: ImageColorのマッピング厳密化)
            val matchColor = state.filterColor == null || kf.color.id == state.filterColor
            matchClass && matchColor
        }
    }

    fun onOverallScrollChanged(startProgress: Float) {
        _uiState.update { 
            val maxStart = (1f - it.overallViewportWidth).coerceAtLeast(0f)
            it.copy(overallScrollStart = startProgress.coerceIn(0f, maxStart))
        }
    }

    fun onDetailedScrollChanged(startProgress: Float) {
        _uiState.update { 
            val maxStart = (1f - it.detailedViewportWidth).coerceAtLeast(0f)
            it.copy(detailedScrollStart = startProgress.coerceIn(0f, maxStart))
        }
    }
}
