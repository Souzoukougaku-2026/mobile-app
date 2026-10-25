package com.example.keyframeplayer.feature.movie.presentation

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
import com.example.keyframeplayer.core.data.repository.ProcessingProgressRepository
import com.example.keyframeplayer.core.data.repository.ProcessingProgressState
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.feature.movie.domain.MovieRepository
import com.example.keyframeplayer.feature.movie.presentation.components.getTickIntervalMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

private fun alignToRoundTime(timeMs: Long, intervalMs: Long): Long {
    if (intervalMs <= 0L) return timeMs
    val tzOffset = TimeZone.getDefault().getOffset(timeMs)
    val localMs = timeMs + tzOffset
    val alignedLocalMs = (localMs / intervalMs) * intervalMs
    return alignedLocalMs - tzOffset
}

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val progressRepository: ProcessingProgressRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val processingProgressState: StateFlow<ProcessingProgressState> = progressRepository.progressState

    private val cropImageIdString: String? = savedStateHandle.get<String>("cropImageId")
    private val initialRealTime: Long = savedStateHandle.get<Long>("realTime") ?: -1L

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private var isInitializedFromList = false
    private var lastSeekTimestamp = 0L

    init {
        fetchStoredKeyframes()
        fetchVideosAndInitialize()
    }

    private fun fetchVideosAndInitialize() {
        viewModelScope.launch {
            repository.ensureVideoEntitiesExist()

            repository.getVideosOrdered().collect { videos ->
                if (videos.isEmpty()) {
                    return@collect
                }

                val offsets = mutableListOf<Long>()
                var currentSum = 0L
                for (v in videos) {
                    offsets.add(currentSum)
                    currentSum += v.durationMs
                }
                val totalMs = currentSum
                val totalSec = (totalMs / 1000f).coerceAtLeast(1f)

                updateRealTimeBounds(videos, _uiState.value.keyframes)

                if (!isInitializedFromList) {
                    isInitializedFromList = true
                    initializePlaybackPosition(videos, offsets, totalSec)
                } else {
                    _uiState.update {
                        it.copy(
                            videos = videos,
                            videoOffsetsMs = offsets,
                            totalDurationSeconds = totalSec
                        )
                    }
                }
                recalculateGraphBars()
            }
        }
    }

    private suspend fun initializePlaybackPosition(
        videos: List<VideoEntity>,
        offsets: List<Long>,
        totalSec: Float
    ) {
        var targetRealTime = initialRealTime
        var targetMoviePath: String? = null
        var targetFileTimeMs: Long? = null

        if (cropImageIdString != null) {
            try {
                val cropImage = repository.getCropImageById(UUID.fromString(cropImageIdString))
                if (cropImage != null) {
                    targetRealTime = cropImage.realTime
                    val keyFrame = repository.getKeyFrameById(cropImage.idKeyFrame)
                    if (keyFrame != null) {
                        targetMoviePath = keyFrame.moviePath
                        targetFileTimeMs = keyFrame.fileTime / 1000L
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var targetIndex = -1
        var posInVideoMs = 0L

        // 1. moviePath と fileTimeMs による正確なマッチング
        if (targetMoviePath != null) {
            targetIndex = videos.indexOfFirst { it.uri == targetMoviePath }
            if (targetIndex != -1 && targetFileTimeMs != null) {
                posInVideoMs = targetFileTimeMs.coerceIn(0L, videos[targetIndex].durationMs)
            }
        }

        // 2. もし moviePath で一致しない場合、realTime によるマッピング
        if (targetIndex == -1 && targetRealTime > 0L) {
            targetIndex = videos.indexOfFirst {
                targetRealTime >= it.realStartTime && targetRealTime <= it.realEndTime
            }
            if (targetIndex == -1) {
                targetIndex = videos.indexOfMinByOrNull { kotlin.math.abs(it.realStartTime - targetRealTime) } ?: 0
            }
            val targetVideo = videos[targetIndex]
            posInVideoMs = (targetRealTime - targetVideo.realStartTime).coerceIn(0L, targetVideo.durationMs)
        }

        if (targetIndex == -1) {
            targetIndex = 0
            posInVideoMs = 0L
        }

        val targetVideo = videos[targetIndex]
        val calculatedRealTime = if (targetRealTime > 0L) targetRealTime else (targetVideo.realStartTime + posInVideoMs)
        val overallMs = offsets[targetIndex] + posInVideoMs
        val overallSec = (overallMs / 1000f).coerceIn(0f, totalSec)

        lastSeekTimestamp = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                videos = videos,
                videoOffsetsMs = offsets,
                totalDurationSeconds = totalSec,
                currentTime = overallSec,
                initialMediaItemIndex = targetIndex,
                initialPositionMs = posInVideoMs,
                currentRealTimeMs = calculatedRealTime,
                seekTarget = SeekTarget(targetIndex, posInVideoMs)
            )
        }
    }

    private fun updateRealTimeBounds(videos: List<VideoEntity>, keyframes: List<CropImage>) {
        val validVideoStarts = videos.map { it.realStartTime }.filter { it > 0L }
        val validVideoEnds = videos.map { it.realEndTime }.filter { it > 0L }

        val minVideo = validVideoStarts.minOrNull()
        val maxVideo = validVideoEnds.maxOrNull()

        val minReal: Long
        val maxReal: Long

        if (minVideo != null && maxVideo != null && maxVideo >= minVideo) {
            minReal = minVideo
            maxReal = maxVideo
        } else {
            val validKeyframeTimes = keyframes.map { it.realTime }.filter { it > 0L }
            val minKf = validKeyframeTimes.minOrNull()
            val maxKf = validKeyframeTimes.maxOrNull()

            if (minKf != null && maxKf != null && maxKf >= minKf) {
                minReal = minKf
                maxReal = maxKf
            } else {
                val now = System.currentTimeMillis()
                minReal = now
                maxReal = now + (_uiState.value.totalDurationSeconds * 1000f).toLong().coerceAtLeast(1000L)
            }
        }

        _uiState.update {
            it.copy(
                minRealTimeMs = minReal,
                maxRealTimeMs = maxReal
            )
        }
    }

    private fun fetchStoredKeyframes() {
        viewModelScope.launch {
            repository.getStoredKeyframes().collect { keyframes ->
                val availableClasses = keyframes.map { it.className }.distinct().sorted()
                _uiState.update {
                    it.copy(
                        keyframes = keyframes,
                        availableClasses = availableClasses
                    )
                }
                updateRealTimeBounds(_uiState.value.videos, keyframes)
                recalculateGraphBars()
            }
        }
    }

    fun onTimeScaleSelected(timeScale: TimeScale) {
        _uiState.update { it.copy(selectedTimeScale = timeScale) }
        recalculateGraphBars()
    }

    fun onClassFilterSelected(className: String?) {
        _uiState.update { it.copy(selectedClassFilter = className) }
        recalculateGraphBars()
    }

    fun onColorFilterSelected(color: ImageColor?) {
        _uiState.update { it.copy(selectedColorFilter = color) }
        recalculateGraphBars()
    }

    fun onLikedFilterToggled() {
        _uiState.update { it.copy(isLikedFilterOnly = !it.isLikedFilterOnly) }
        recalculateGraphBars()
    }

    fun onKeyframeLikedStateChanged(id: UUID, isLiked: Boolean) {
        viewModelScope.launch {
            repository.updateCropImageLiked(id, isLiked)
        }
    }

    private fun recalculateGraphBars() {
        val state = _uiState.value
        val keyframes = state.keyframes
        val timeScale = state.selectedTimeScale
        val classFilter = state.selectedClassFilter
        val colorFilter = state.selectedColorFilter
        val likedFilterOnly = state.isLikedFilterOnly
        val intervalMs = timeScale.getTickIntervalMs()

        val rawMinReal = state.minRealTimeMs
        val scaleWindowMs = (timeScale.totalSeconds * 1000f).toLong().coerceAtLeast(1000L)
        val rawSpanMs = maxOf(state.totalRealDurationMs, scaleWindowMs)
        val rawMaxReal = rawMinReal + rawSpanMs

        // 目盛り時間帯（正時）にアライン
        val alignedMinReal = alignToRoundTime(rawMinReal, intervalMs)
        var alignedMaxReal = alignToRoundTime(rawMaxReal, intervalMs)
        if (alignedMaxReal < alignedMinReal + scaleWindowMs) {
            alignedMaxReal = alignedMinReal + scaleWindowMs
        }
        val spanMs = (alignedMaxReal - alignedMinReal).toDouble()

        // 1. 条件（クラス・色・いいね）で CropImage を絞り込み
        val filtered = keyframes.filter { item ->
            (classFilter == null || item.className == classFilter) &&
            (colorFilter == null || item.color == colorFilter) &&
            (!likedFilterOnly || item.isLiked)
        }

        // 2. 1画面幅(scaleWindowMs)をぴったり90分割するバケット幅(ms)の計算
        val bucketMs = (scaleWindowMs.toDouble() / 90.0).coerceAtLeast(1.0)
        val totalBarCount = (spanMs / bucketMs).toInt().coerceAtLeast(90)
        val counts = IntArray(totalBarCount)

        for (item in filtered) {
            val diffMs = (item.realTime - alignedMinReal).toDouble()
            if (diffMs >= 0.0) {
                val index = (diffMs / bucketMs).toInt()
                if (index in 0 until totalBarCount) {
                    counts[index]++
                }
            }
        }

        val countsList = counts.toList()
        val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1

        _uiState.update {
            it.copy(
                graphBarCounts = countsList,
                maxBarCount = maxCount
            )
        }
    }

    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(videoUri = uri, isLoading = true) }

            val result = repository.processVideo(uri)

            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * 現在の再生位置から指定秒数分だけスキップ（前送り/巻き戻し）します。
     * 動画ファイルの境界を跨いで時間軸上で移動します。
     * @param deltaSeconds 移動する秒数（例: +5.0f, -5.0f）
     */
    fun onSkipBy(deltaSeconds: Float) {
        val currentSec = _uiState.value.currentTime
        val totalSec = _uiState.value.totalDurationSeconds
        val targetSec = (currentSec + deltaSeconds).coerceIn(0f, totalSec)
        onTimeChanged(targetSec)
    }

    /**
     * 指定された時間に現在の再生位置を変更します（シークバー・グラフ操作時）。
     * @param newTime 変更後の時間（秒）
     */
    fun onTimeChanged(newTime: Float) {
        lastSeekTimestamp = System.currentTimeMillis()
        val totalSec = _uiState.value.totalDurationSeconds
        val clampedTime = newTime.coerceIn(0f, totalSec)
        val videos = _uiState.value.videos
        val offsets = _uiState.value.videoOffsetsMs

        val (index, posMs, realTimeMs) = mapOverallTimeToVideoPosition(clampedTime, videos, offsets)

        _uiState.update {
            it.copy(
                currentTime = clampedTime,
                currentRealTimeMs = realTimeMs,
                seekTarget = SeekTarget(index, posMs)
            )
        }
    }

    /**
     * ExoPlayer からの現在の再生位置更新（1/200ms間隔等で呼び出し）
     */
    fun onPlayerPositionUpdated(mediaItemIndex: Int, positionMs: Long) {
        if (System.currentTimeMillis() - lastSeekTimestamp < 800L) {
            return
        }

        val videos = _uiState.value.videos
        val offsets = _uiState.value.videoOffsetsMs
        if (videos.isEmpty() || mediaItemIndex !in videos.indices) return

        val offsetMs = offsets.getOrNull(mediaItemIndex) ?: 0L
        val overallMs = offsetMs + positionMs
        val overallSec = (overallMs / 1000f).coerceIn(0f, _uiState.value.totalDurationSeconds)

        val targetVideo = videos[mediaItemIndex]
        val realTimeMs = targetVideo.realStartTime + positionMs.coerceIn(0L, targetVideo.durationMs)

        _uiState.update {
            it.copy(
                currentTime = overallSec,
                currentRealTimeMs = realTimeMs
            )
        }
    }

    fun onProgressTapped(progress: Float) {
        val state = _uiState.value
        val minRealTimeMs = state.minRealTimeMs
        val scaleWindowMs = (state.selectedTimeScale.totalSeconds * 1000f).toLong().coerceAtLeast(1000L)
        val spanMs = maxOf(state.totalRealDurationMs, scaleWindowMs)
        val targetRealTimeMs = minRealTimeMs + (progress.coerceIn(0f, 1f) * spanMs).toLong()

        val videos = state.videos
        val offsets = state.videoOffsetsMs
        if (videos.isEmpty()) {
            _uiState.update { it.copy(currentRealTimeMs = targetRealTimeMs) }
            return
        }

        var index = videos.indexOfFirst { targetRealTimeMs >= it.realStartTime && targetRealTimeMs <= it.realEndTime }
        if (index == -1) {
            index = videos.indexOfMinByOrNull {
                minOf(
                    kotlin.math.abs(it.realStartTime - targetRealTimeMs),
                    kotlin.math.abs(it.realEndTime - targetRealTimeMs)
                )
            } ?: 0
        }

        val targetVideo = videos[index]
        val posMs = (targetRealTimeMs - targetVideo.realStartTime).coerceIn(0L, targetVideo.durationMs)
        val overallMs = (offsets.getOrNull(index) ?: 0L) + posMs
        val overallSec = (overallMs / 1000f).coerceIn(0f, state.totalDurationSeconds)

        lastSeekTimestamp = System.currentTimeMillis()

        _uiState.update {
            it.copy(
                currentTime = overallSec,
                currentRealTimeMs = targetRealTimeMs,
                seekTarget = SeekTarget(index, posMs)
            )
        }
    }

    fun onVisibleRangeChanged(startProgress: Float) {
        // 後方互換維持
    }

    private fun mapOverallTimeToVideoPosition(
        timeSec: Float,
        videos: List<VideoEntity>,
        offsets: List<Long>
    ): Triple<Int, Long, Long> {
        if (videos.isEmpty() || offsets.isEmpty()) {
            return Triple(0, 0L, 0L)
        }
        val targetMs = (timeSec * 1000f).toLong()

        var index = offsets.indexOfLast { targetMs >= it }
        if (index == -1) index = 0
        if (index >= videos.size) index = videos.lastIndex

        val offset = offsets[index]
        val video = videos[index]
        val posMs = (targetMs - offset).coerceIn(0L, video.durationMs)
        val realTimeMs = video.realStartTime + posMs

        return Triple(index, posMs, realTimeMs)
    }

    private inline fun <T> List<T>.indexOfMinByOrNull(selector: (T) -> Long): Int? {
        if (isEmpty()) return null
        var minIdx = 0
        var minValue = selector(this[0])
        for (i in 1 until size) {
            val v = selector(this[i])
            if (v < minValue) {
                minValue = v
                minIdx = i
            }
        }
        return minIdx
    }
}
