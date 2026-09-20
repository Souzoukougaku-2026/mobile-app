package com.example.keyframeplayer.ui.movie

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.keyframeplayer.ui.movie.components.*
import com.example.keyframeplayer.model.CropImage
import com.example.keyframeplayer.model.ImageColor
import com.example.keyframeplayer.util.VideoInfo

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieRoute(
    viewModel: MovieViewModel,
    onBack: () -> Unit // 追加
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
        onFilterChanged = viewModel::onFilterChanged, // 追加
        onBack = onBack // 追加
    )
}

@androidx.media3.common.util.UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
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
    onFilterChanged: (String?, Int?) -> Unit,
    onBack: () -> Unit
) {
    val overallScrollState = rememberScrollState()
    val detailedScrollState = rememberScrollState()
    var isFullScreen by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()

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
        // --- 上部バー (TopAppBar) ---
        if (!isFullScreen) {
            TopAppBar(
                title = { Text("Video Details", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) { // 明示的に呼び出し
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Filter Settings")
                    }
                }
            )
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = filterSheetState
            ) {
                FilterSheetContent(
                    currentClass = uiState.filterClass,
                    currentColorId = uiState.filterColor,
                    onFilterChanged = onFilterChanged,
                    onDismiss = { showFilterSheet = false }
                )
            }
        }

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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterSheetContent(
    currentClass: String?,
    currentColorId: Int?,
    onFilterChanged: (String?, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("グラフの絞り込み", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            TextButton(onClick = { 
                onFilterChanged(null, null)
                onDismiss()
            }) {
                Text("リセット")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // クラス絞り込みセクション
        Text("オブジェクトの種類", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val classes = listOf("all", "wallet", "headphone", "key", "smart phone", "umbrella")
            classes.forEach { cls ->
                val isSelected = (cls == "all" && currentClass == null) || (cls == currentClass)
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(if (cls == "all") null else cls, currentColorId) },
                    label = { Text(cls.uppercase()) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 色絞り込みセクション
        Text("検出された色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val colors = listOf(
                "すべて" to null,
                "白" to 0,
                "銀" to 1,
                "黒" to 3,
                "赤" to 4,
                "青" to 12,
                "黄" to 6,
                "緑" to 9
            )
            colors.forEach { (name, id) ->
                val isSelected = (id == currentColorId)
                val colorHex = when(id) {
                    0 -> Color.White
                    1 -> Color(0xFFC0C0C0)
                    3 -> Color.Black
                    4 -> Color.Red
                    12 -> Color.Blue
                    6 -> Color.Yellow
                    9 -> Color.Green
                    else -> Color.Transparent
                }
                
                InputChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(currentClass, id) },
                    label = { Text(name) },
                    avatar = if (id != null) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(colorHex, shape = CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape)
                            )
                        }
                    } else null,
                    leadingIcon = if (isSelected && id == null) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
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

