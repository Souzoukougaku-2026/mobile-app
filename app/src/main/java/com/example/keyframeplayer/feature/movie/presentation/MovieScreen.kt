package com.example.keyframeplayer.feature.movie.presentation

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import com.example.keyframeplayer.core.data.repository.ProcessingProgressState
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.feature.movie.presentation.components.*
import com.example.keyframeplayer.ui.component.ProcessingProgressBar
import java.util.UUID
import kotlinx.coroutines.delay

@Composable
fun MovieRoute(
    viewModel: MovieViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val processingState by viewModel.processingProgressState.collectAsStateWithLifecycle()

    MovieScreen(
        uiState = uiState,
        processingProgressState = processingState,
        onNavigateBack = onNavigateBack,
        onTimeChanged = viewModel::onTimeChanged,
        onSkipBy = viewModel::onSkipBy,
        onProgressTapped = viewModel::onProgressTapped,
        onPlayerPositionUpdated = viewModel::onPlayerPositionUpdated,
        onTimeScaleSelected = viewModel::onTimeScaleSelected,
        onClassFilterSelected = viewModel::onClassFilterSelected,
        onColorFilterSelected = viewModel::onColorFilterSelected,
        onLikedFilterToggled = viewModel::onLikedFilterToggled,
        onKeyframeLikedStateChanged = viewModel::onKeyframeLikedStateChanged
    )
}

@OptIn(UnstableApi::class)
@Composable
fun MovieScreen(
    uiState: MovieUiState,
    processingProgressState: ProcessingProgressState = ProcessingProgressState(),
    onNavigateBack: () -> Unit = {},
    onTimeChanged: (Float) -> Unit,
    onSkipBy: (Float) -> Unit,
    onProgressTapped: (Float) -> Unit,
    onPlayerPositionUpdated: (Int, Long) -> Unit,
    onTimeScaleSelected: (TimeScale) -> Unit,
    onClassFilterSelected: (String?) -> Unit,
    onColorFilterSelected: (ImageColor?) -> Unit,
    onLikedFilterToggled: () -> Unit = {},
    onKeyframeLikedStateChanged: (UUID, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isFullScreen by remember { mutableStateOf(false) }

    // 単一の ExoPlayer インスタンスを作成し、画面のライフサイクルで管理
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    // 動画リスト (MediaItems) のセットアップ
    // 自動再生はしない: 開始位置にシークした状態で停止しておき、再生はユーザーの操作で始める
    LaunchedEffect(uiState.videos) {
        if (uiState.videos.isNotEmpty()) {
            val mediaItems = uiState.videos.map { video ->
                MediaItem.fromUri(Uri.parse(video.uri))
            }
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            if (uiState.initialMediaItemIndex in uiState.videos.indices) {
                exoPlayer.seekTo(uiState.initialMediaItemIndex, uiState.initialPositionMs)
            }
        } else if (uiState.videoUri != null && uiState.videoUri != Uri.EMPTY) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uiState.videoUri))
            exoPlayer.prepare()
        }
    }

    // ViewModelからのシーク指示のハンドリング
    LaunchedEffect(uiState.seekTarget) {
        val target = uiState.seekTarget ?: return@LaunchedEffect
        if (target.mediaItemIndex in 0 until exoPlayer.mediaItemCount) {
            exoPlayer.seekTo(target.mediaItemIndex, target.positionMs)
        }
    }

    // プレイヤーの再生位置を ViewModel へ定期的に同期
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                val idx = exoPlayer.currentMediaItemIndex
                val posMs = exoPlayer.currentPosition
                onPlayerPositionUpdated(idx, posMs)
            }
            delay(200)
        }
    }

    // 現在時刻に最も近いアクティブキーフレームの検索
    val activeKeyframe = remember(uiState.keyframes, uiState.currentRealTimeMs) {
        if (uiState.keyframes.isEmpty()) null
        else {
            uiState.keyframes.minByOrNull { kotlin.math.abs(it.realTime - uiState.currentRealTimeMs) }
        }
    }

    // シーク操作ハンドラ: タップ位置をそのまま ViewModel へ渡す。
    // 動画の無いエリアの場合は ViewModel 側で最寄りの動画端に寄せて処理する (通知は出さない)
    val handleProgressTapped: (Float) -> Unit = { overallProgress ->
        onProgressTapped(overallProgress)
    }

    Scaffold(
        bottomBar = {
            ProcessingProgressBar(
                isProcessing = processingProgressState.isProcessing,
                progress = processingProgressState.progress,
                statusText = processingProgressState.statusText,
                // 画面下端に置くため、ナビゲーションバー分の余白を中身に持たせる
                applyNavigationBarPadding = true
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isFullScreen) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            // --- ヘッダーエリア ---
            if (!isFullScreen) {
                MovieHeader(onNavigateBack = onNavigateBack)
            }

            // --- ビデオ表示エリア ---
            // 動画が表示されない余白部分は真っ黒にする (テーマ背景色が見えないように)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .then(
                        if (isFullScreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f)
                    )
            ) {
                VideoPlayerContainer(
                    exoPlayer = exoPlayer,
                    isFullScreen = isFullScreen,
                    onToggleFullScreen = { isFullScreen = !isFullScreen },
                    onSkipBy = onSkipBy,
                    keyframes = uiState.keyframes
                )
            }

            if (!isFullScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    // --- 統合グラフ (Unified Graph Area) ---
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "検出データ集計グラフ",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )

                        UnifiedGraphArea(
                            uiState = uiState,
                            onProgressTapped = handleProgressTapped,
                            onTimeScaleSelected = onTimeScaleSelected
                        )

                        GraphControlArea(
                            uiState = uiState,
                            onTimeScaleSelected = onTimeScaleSelected,
                            onClassFilterSelected = onClassFilterSelected,
                            onColorFilterSelected = onColorFilterSelected,
                            onLikedFilterToggled = onLikedFilterToggled
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TimeLabel(
                        currentTime = uiState.currentTime,
                        totalDuration = uiState.totalDurationSeconds,
                        realTimeMs = uiState.currentRealTimeMs,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BottomInfoArea(
                        currentTime = uiState.currentTime,
                        activeKeyframe = activeKeyframe,
                        keyframes = uiState.keyframes,
                        selectedClassFilter = uiState.selectedClassFilter,
                        selectedColorFilter = uiState.selectedColorFilter,
                        isLikedFilterOnly = uiState.isLikedFilterOnly,
                        realTimeMs = uiState.currentRealTimeMs,
                        onKeyframeLikedStateChanged = onKeyframeLikedStateChanged,
                        onSeekToKeyframe = { cropImage ->
                            val progress = if (uiState.totalRealDurationMs > 0L) {
                                ((cropImage.realTime - uiState.minRealTimeMs).toFloat() / uiState.totalRealDurationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            handleProgressTapped(progress)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 画面上部のヘッダー (戻るボタン、OSステータスバーとの重なりを防止)
 * 戻るボタンの連打は 800ms 以内を無視する。
 */
@Composable
private fun MovieHeader(onNavigateBack: () -> Unit) {
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackClickTime >= 800L) {
                        lastBackClickTime = currentTime
                        onNavigateBack()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る"
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

/**
 * 動画上オーバーレイの ±10秒 スキップボタン
 */
@Composable
private fun SkipOverlayButton(text: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * プレイヤーの設定ボタン用、再生速度の選択メニューを表示する
 */
private fun showPlaybackSpeedMenu(context: Context, anchor: View, exoPlayer: ExoPlayer) {
    val popup = PopupMenu(context, anchor)
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    speeds.forEach { speed ->
        popup.menu.add("${speed}x").setOnMenuItemClickListener {
            exoPlayer.playbackParameters = PlaybackParameters(speed)
            true
        }
    }
    popup.show()
}

/**
 * 連続ストリーミング用 PlayerView コンテナ (動画内ジェスチャー & オーバーレイ操作対応)
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerContainer(
    exoPlayer: ExoPlayer,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onSkipBy: (Float) -> Unit,
    keyframes: List<CropImage>
) {
    var isControllerVisible by remember { mutableStateOf(false) }

    // ダブルタップのビジュアルフィードバック状態
    var showRewindIndicator by remember { mutableStateOf(false) }
    var showForwardIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(showRewindIndicator) {
        if (showRewindIndicator) {
            delay(800)
            showRewindIndicator = false
        }
    }

    LaunchedEffect(showForwardIndicator) {
        if (showForwardIndicator) {
            delay(800)
            showForwardIndicator = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 2f) {
                            // 左半分ダブルタップ: -10秒戻し
                            showRewindIndicator = true
                            onSkipBy(-10f)
                        } else {
                            // 右半分ダブルタップ: +10秒送り
                            showForwardIndicator = true
                            onSkipBy(10f)
                        }
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    // フレームが無い部分 (レターボックスや再生前のシャッター) は真っ黒にする
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        isControllerVisible = (visibility == View.VISIBLE)
                        if (visibility == View.VISIBLE) {
                            val timeBar = findViewById<DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
                            if (timeBar != null) {
                                val markerTimes = keyframes.map { it.realTime }.toLongArray()
                                timeBar.setAdGroupTimesMs(
                                    markerTimes,
                                    BooleanArray(markerTimes.size),
                                    markerTimes.size
                                )
                                timeBar.setAdMarkerColor(android.graphics.Color.RED)
                            }

                            // 早送り・巻き戻しボタンをファイル境界対応の 10秒処理にオーバーライド
                            val ffwdButton = findViewById<View>(androidx.media3.ui.R.id.exo_ffwd)
                            ffwdButton?.setOnClickListener {
                                showForwardIndicator = true
                                onSkipBy(10f)
                            }
                            val rewButton = findViewById<View>(androidx.media3.ui.R.id.exo_rew)
                            rewButton?.setOnClickListener {
                                showRewindIndicator = true
                                onSkipBy(-10f)
                            }

                            val fullScreenButton = findViewById<View>(androidx.media3.ui.R.id.exo_fullscreen)
                            fullScreenButton?.setOnClickListener {
                                onToggleFullScreen()
                            }
                            val settingsButton = findViewById<View>(androidx.media3.ui.R.id.exo_settings)
                            settingsButton?.setOnClickListener { view ->
                                showPlaybackSpeedMenu(ctx, view, exoPlayer)
                            }
                        }
                    })
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        // コントローラー表示時の動画上オーバーレイボタン (-10秒 / +10秒)
        if (isControllerVisible) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.6f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkipOverlayButton(
                    text = "-10s",
                    onClick = {
                        showRewindIndicator = true
                        onSkipBy(-10f)
                    }
                )

                SkipOverlayButton(
                    text = "+10s",
                    onClick = {
                        showForwardIndicator = true
                        onSkipBy(10f)
                    }
                )
            }
        }

        // ダブルタップ時の画面上フラッシュインジケーター (左: -10秒戻し)
        if (showRewindIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "⏪ -10秒",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // ダブルタップ時の画面上フラッシュインジケーター (右: +10秒送り)
        if (showForwardIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .background(Color.White.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+10秒 ⏩",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // フルスクリーン切り替えボタン
        if (isControllerVisible) {
            IconButton(
                onClick = { onToggleFullScreen() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 56.dp, bottom = 4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    tint = Color.White
                )
            }
        }
    }
}
