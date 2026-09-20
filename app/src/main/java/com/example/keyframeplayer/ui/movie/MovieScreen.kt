package com.example.keyframeplayer.ui.movie

import androidx.annotation.OptIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.keyframeplayer.ui.movie.components.*
import com.example.keyframeplayer.model.CropImage
import com.example.keyframeplayer.util.VideoInfo

@Composable
fun MovieRoute(
    viewModel: MovieViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val seekRequest by viewModel.seekRequest.collectAsStateWithLifecycle()
    
    MovieScreen(
        uiState = uiState,
        seekRequest = seekRequest,
        onSeekProcessed = viewModel::onSeekProcessed,
        onTimeChanged = { time -> viewModel.onTimeChanged(time, seekPlayer = false) },
        onSeekStarted = viewModel::onSeekStarted,
        onSeekFinished = viewModel::onSeekFinished,
        onSkipTime = viewModel::skipTime,
        onPlayerUpdate = viewModel::onPlayerPositionChanged,
        onProgressTapped = viewModel::onProgressTapped,
        onOverallScrollChanged = viewModel::onOverallScrollChanged,
        onDetailedScrollChanged = viewModel::onDetailedScrollChanged,
    )
}

@OptIn(UnstableApi::class)
@Composable
fun MovieScreen(
    uiState: MovieUiState,
    seekRequest: MovieViewModel.SeekEvent?,
    onSeekProcessed: () -> Unit,
    onTimeChanged: (Float) -> Unit,
    onSeekStarted: () -> Unit,
    onSeekFinished: (Float) -> Unit,
    onSkipTime: (Float) -> Unit,
    onPlayerUpdate: (Int, Long) -> Unit,
    onProgressTapped: (Float) -> Unit,
    onOverallScrollChanged: (Float) -> Unit,
    onDetailedScrollChanged: (Float) -> Unit,
) {
    val overallScrollState = rememberScrollState()
    val detailedScrollState = rememberScrollState()
    var isFullScreen by remember { mutableStateOf(false) }

    // 再生時間と詳細グラフのスクロール位置を同期 (ユーザーが触っていない時)
    LaunchedEffect(uiState.currentTime) {
        if (!detailedScrollState.isScrollInProgress) {
            val totalWidth = detailedScrollState.maxValue + detailedScrollState.viewportSize
            if (totalWidth > 0) {
                val progress = uiState.currentTime / uiState.totalDurationSeconds
                val target = (progress * totalWidth) - (detailedScrollState.viewportSize / 2f)
                detailedScrollState.scrollTo(target.toInt().coerceIn(0, detailedScrollState.maxValue))
            }
        }
        // 全体グラフも同様に同期
        if (!overallScrollState.isScrollInProgress) {
            val totalWidth = overallScrollState.maxValue + overallScrollState.viewportSize
            if (totalWidth > 0) {
                val progress = uiState.currentTime / uiState.totalDurationSeconds
                val target = (progress * totalWidth) - (overallScrollState.viewportSize / 2f)
                overallScrollState.scrollTo(target.toInt().coerceIn(0, overallScrollState.maxValue))
            }
        }
    }

    // 全体グラフのスクロール位置を監視
    LaunchedEffect(overallScrollState) {
        snapshotFlow { 
            val total = overallScrollState.maxValue + overallScrollState.viewportSize
            if (total > 0) overallScrollState.value.toFloat() / total else 0f
        }
        .distinctUntilChanged { old, new -> kotlin.math.abs(old - new) < 0.001f }
        .collect { onOverallScrollChanged(it) }
    }

    // 詳細グラフのスクロール位置を監視
    LaunchedEffect(detailedScrollState) {
        snapshotFlow { 
            val total = detailedScrollState.maxValue + detailedScrollState.viewportSize
            if (total > 0) detailedScrollState.value.toFloat() / total else 0f
        }
        .distinctUntilChanged { old, new -> kotlin.math.abs(old - new) < 0.001f }
        .collect { onDetailedScrollChanged(it) }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(if (isFullScreen) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isFullScreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
        ) {
            ContinuousVideoPlayer(
                videoInfos = uiState.selectedVideos,
                seekRequest = seekRequest,
                onSeekProcessed = onSeekProcessed,
                onPlayerUpdate = onPlayerUpdate,
                isFullScreen = isFullScreen,
                onToggleFullScreen = { isFullScreen = !isFullScreen },
                onSkipTime = onSkipTime,
                currentTime = uiState.currentTime,
                totalDuration = uiState.totalDurationSeconds,
                onTimeChanged = onTimeChanged,
                onSeekStarted = onSeekStarted,
                onSeekFinished = onSeekFinished,
                keyframes = uiState.keyframes,
                sessionStartTimeMs = uiState.sessionStartTimeMs
            )
        }

        if (!isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. 全体グラフ (6時間表示)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "全体グラフ (6h Viewport)",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OverallScrollableArea(
                        uiState = uiState,
                        scrollState = overallScrollState,
                        onProgressTapped = onProgressTapped
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. 詳細グラフ (1時間表示)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "詳細グラフ (1h Viewport)",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    DetailedScrollableArea(
                        uiState = uiState,
                        scrollState = detailedScrollState,
                        onTimeChanged = { onTimeChanged(it) }, // ここはスライダー用
                        onProgressTapped = onProgressTapped
                    )
                }

                TimeLabel(
                    uiState.currentTime,
                    uiState.totalDurationSeconds,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                BottomInfoArea(
                    currentTime = uiState.currentTime,
                    thumbnailPath = uiState.selectedThumbnailPath,
                    bbox = uiState.selectedBBox
                )
            }
        }
    }
}

@UnstableApi
@Composable
fun ContinuousVideoPlayer(
    videoInfos: List<VideoInfo>,
    seekRequest: MovieViewModel.SeekEvent?,
    onSeekProcessed: () -> Unit,
    onPlayerUpdate: (Int, Long) -> Unit,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onSkipTime: (Float) -> Unit,
    currentTime: Float,
    totalDuration: Float,
    onTimeChanged: (Float) -> Unit,
    onSeekStarted: () -> Unit,
    onSeekFinished: (Float) -> Unit,
    keyframes: List<CropImage>,
    sessionStartTimeMs: Long
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(videoInfos) {
        exoPlayer.clearMediaItems()
        val mediaItems = videoInfos.map { androidx.media3.common.MediaItem.fromUri(it.uri) }
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    LaunchedEffect(seekRequest) {
        seekRequest?.let {
            exoPlayer.seekTo(it.index, it.positionMs)
            onSeekProcessed()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                onPlayerUpdate(exoPlayer.currentMediaItemIndex, exoPlayer.currentPosition)
            }
            kotlinx.coroutines.delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { onToggleFullScreen() },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Fullscreen",
                        tint = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                IconButton(
                    onClick = { onSkipTime(-10f) },
                    modifier = Modifier.size(64.dp).background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Back 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    modifier = Modifier.size(64.dp).background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(
                    onClick = { onSkipTime(10f) },
                    modifier = Modifier.size(64.dp).background(Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(bottom = 8.dp)
            ) {
                Slider(
                    value = currentTime,
                    onValueChange = { 
                        onSeekStarted()
                        onTimeChanged(it) 
                    },
                    onValueChangeFinished = { onSeekFinished(currentTime) },
                    valueRange = 0f..totalDuration,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(32.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red,
                        inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
