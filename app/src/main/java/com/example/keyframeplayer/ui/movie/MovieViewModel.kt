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
        // データ本数が変わるので再取得
        fetchBarData()
        fetchDetailedBarData()
    }

    fun setSelectedThumbnail(path: String?, bbox: android.graphics.RectF? = null) {
        _uiState.update { it.copy(
            selectedThumbnailPath = path,
            selectedBBox = bbox
        ) }
    }

    private fun fetchKeyframes() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val clopEntities = clopImageDao.getAllClopImages()
                val keyframes = clopEntities.mapNotNull { clop ->
                    val keyFrame = keyFrameDao.getKeyFrameById(clop.idKeyFrame)
                    keyFrame?.let { kf ->
                        CropImage(
                            id = clop.uuid,
                            className = clop.classname,
                            score = clop.score.toDouble(),
                            color = ImageColor.White,
                            bboxLeft = clop.bboxPoint.ulX.toInt(),
                            bboxTop = clop.bboxPoint.ulY.toInt(),
                            bboxRight = clop.bboxPoint.lrX.toInt(),
                            bboxBottom = clop.bboxPoint.lrY.toInt(),
                            realTime = kf.realTime,
                            idKeyFrame = kf.id
                        )
                    }
                }
                _uiState.update { it.copy(keyframes = keyframes) }
            }
        }
    }

    private fun fetchBarData() {
        val barCount = _uiState.value.totalOverallBarCount
        val mockData = List(barCount) { kotlin.random.Random.nextFloat() }
        _uiState.update { it.copy(barValues = mockData) }
    }

    private fun fetchDetailedBarData() {
        val barCount = _uiState.value.totalDetailedBarCount
        val mockData = List(barCount) { kotlin.random.Random.nextFloat() }
        _uiState.update { it.copy(detailedBarValues = mockData) }
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
