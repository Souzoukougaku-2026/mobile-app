package com.example.a2026souzou.feature.movie.presentation

import androidx.annotation.OptIn
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import com.example.a2026souzou.feature.movie.presentation.components.*
import com.example.a2026souzou.core.domain.model.CropImage

@Composable
fun MovieRoute(
    viewModel: MovieViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    MovieScreen(
        uiState = uiState,
        onTimeChanged = viewModel::onTimeChanged,
        onProgressTapped = viewModel::onProgressTapped,
        onVisibleRangeChanged = viewModel::onVisibleRangeChanged,
    )
}

@OptIn(ExperimentalFoundationApi::class, UnstableApi::class) // ← UnstableApi::class を追加
@Composable
fun MovieScreen(
    uiState: MovieUiState,
    onTimeChanged: (Float) -> Unit,
    onProgressTapped: (Float) -> Unit,
    onVisibleRangeChanged: (Float) -> Unit,
) {
    val scrollState = rememberScrollState()
    // フルスクリーン状態の管理
    var isFullScreen by remember { mutableStateOf(false) }

    // ユーザーが手動でスクロールしていない時だけ動作する
    LaunchedEffect(uiState.currentTime) {
        if (!scrollState.isScrollInProgress) {
            val totalWidthPx = scrollState.maxValue + scrollState.viewportSize
            if (totalWidthPx > 0) {
                // 現在の時間が画面中央にくるようなスクロール位置を計算
                val progress = uiState.currentTime / uiState.totalDurationSeconds
                val targetScroll = (progress * totalWidthPx) - (scrollState.viewportSize / 2f)

                // 滑らかにスクロール（animateScrollTo）
                scrollState.scrollTo(targetScroll.toInt().coerceIn(0, scrollState.maxValue))
            }
        }
    }

    // PagerState: キーフレーム（検出物体）ごとにページを分ける設定
    // initialPage は現在の再生時間に最も近いキーフレームに設定
    val pagerState = rememberPagerState(
        initialPage = remember(uiState.keyframes) {
            val index = uiState.keyframes.indexOfFirst { it.realTime.toFloat() / 1000f >= uiState.currentTime }
            if (index != -1) index else 0
        },
        pageCount = { uiState.keyframes.size }
    )

    
    // スクロール位置の監視を最適化 (ANR対策)
    LaunchedEffect(scrollState) {
        snapshotFlow { 
            val totalContentWidth = scrollState.maxValue + scrollState.viewportSize
            if (totalContentWidth > 0) scrollState.value.toFloat() / totalContentWidth else 0f
        }
        .distinctUntilChanged { old: Float, new: Float ->
            // 変化が非常に小さい場合は無視して再描画を抑える (0.1% 未満の変化は無視)
            kotlin.math.abs(old - new) < 0.001f 
        }
        .collect { progress ->
            onVisibleRangeChanged(progress)

            if (scrollState.isScrollInProgress) {
                onTimeChanged(progress * uiState.totalDurationSeconds)
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(if (isFullScreen) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        // --- ビデオ表示エリア ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // フルスクリーン時は画面全体(weight 1)、通常時は16:9の比率にする
                    if (isFullScreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f)
                )
        ) {
            // キーフレームごとにプレイヤーを切り替える Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                // videoUri がある場合のみ VideoPlayerItem を表示
                uiState.videoUri?.let { uri ->
                    VideoPlayerItem(
                        uri = uri,
                        timeUs = uiState.keyframes[page].realTime * 1000L,
                        isActive = (pagerState.currentPage == page),
                        isFullScreen = isFullScreen,
                        onToggleFullScreen = { isFullScreen = !isFullScreen },
                        keyframes = uiState.keyframes,
                        onTimeChanged = onTimeChanged
                    )
                }
            }
        }

        if (!isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. 全体グラフ (Overall Graph)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "全体グラフ (Overall Graph)",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    OverallGraphArea(uiState = uiState, onProgressTapped = onProgressTapped)

                    OverallTimeTicks(uiState.totalDurationSeconds)

                    SlimSlider(
                        value = uiState.currentTime,
                        onValueChange = onTimeChanged,
                        valueRange = 0f..uiState.totalDurationSeconds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. 詳細グラフ (Detailed Graph)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "詳細グラフ (Detailed Graph)",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    DetailedScrollableArea(
                        uiState = uiState,
                        scrollState = scrollState,
                        onTimeChanged = onTimeChanged,
                        onProgressTapped = onProgressTapped
                    )
                }

                TimeLabel(
                    uiState.currentTime,
                    uiState.totalDurationSeconds,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                BottomInfoArea(uiState.currentTime)
            }
        }
    }
}

/**
 * 個別ビデオプレイヤー項目
 */
@androidx.media3.common.util.UnstableApi
@Composable
fun VideoPlayerItem(
    uri: android.net.Uri,
    timeUs: Long,
    isActive: Boolean,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    keyframes: List<CropImage>,
    onTimeChanged: (Float) -> Unit // ViewModel への時間通知用に追加
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        }
    }

    // 動画の準備とシーク
    LaunchedEffect(uri, timeUs) {
        val mediaItem = androidx.media3.common.MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(timeUs / 1000L)
        exoPlayer.playWhenReady = isActive
    }

    // プレイヤーの再生位置を ViewModel (グラフ) に同期させる
    LaunchedEffect(isActive, exoPlayer) {
        if (isActive) {
            while (true) {
                if (exoPlayer.isPlaying) {
                    onTimeChanged(exoPlayer.currentPosition / 1000f)
                }
                kotlinx.coroutines.delay(200) // 200ms間隔で更新
            }
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
                    useController = true
                    // シークバーのマーカー表示ロジック
                    setControllerVisibilityListener(androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
                        if (visibility == android.view.View.VISIBLE) {
                            val timeBar = findViewById<androidx.media3.ui.DefaultTimeBar>(androidx.media3.ui.R.id.exo_progress)
                            if (timeBar != null) {
                                val markerTimes = keyframes.map { it.realTime }.toLongArray()
                                timeBar.setAdGroupTimesMs(markerTimes, BooleanArray(markerTimes.size), markerTimes.size)
                                timeBar.setAdMarkerColor(android.graphics.Color.RED)
                            }
                        }
                    })
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        // フルスクリーン切り替えボタン
        IconButton(
            onClick = { onToggleFullScreen() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
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
