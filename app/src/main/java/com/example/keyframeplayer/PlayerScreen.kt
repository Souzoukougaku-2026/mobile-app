package com.example.keyframeplayer

import android.graphics.Color
import android.net.Uri
import kotlin.OptIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R


@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Composable
fun PlayerScreen(
    viewModel: SharedViewModel,
    timeUs: Long
) {
    //val context = LocalContext.current
    val uri by viewModel.selectedUri.collectAsState()
    val keyframes by viewModel.keyframeItems.collectAsState()

    var  isFullScreen by remember {mutableStateOf(false)}
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val currentUri = uri

    if (keyframes.isEmpty() || currentUri == null) return

    //val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    val pagerState = rememberPagerState(
        initialPage = remember(keyframes, timeUs) {
            val index = keyframes.indexOfFirst { it.timeUs == timeUs }
            if (index != -1) index else 0
        },
        pageCount = { keyframes.size }
    )



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .then(if (isFullScreen) Modifier else Modifier.statusBarsPadding())
    ) {
        if (selectedTabIndex == 0 || isFullScreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFullScreen) Modifier.weight(1f)
                        else Modifier.aspectRatio(16f / 9f)
                    )
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    currentUri?.let {
                        VideoPlayerItem(
                            uri = it,
                            timeUs = keyframes[page].timeUs,
                            isActive = (pagerState.currentPage == page),
                            isFullScreen = isFullScreen,
                            onToggleFullScreen = { isFullScreen = !isFullScreen },
                            keyframes = keyframes
                        )
                    }
                }
            }
        }

        if (!isFullScreen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(16.dp)
            ) {
            }
            //TODO動画画面下部UI
        }
    }
}


@UnstableApi
@Composable
fun VideoPlayerItem(
    uri: Uri,
    timeUs: Long,
    isActive: Boolean,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    keyframes: List<KeyframeItem>,
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    LaunchedEffect(uri, timeUs) {
        //val currentUri = uri ?: return@LaunchedEffect
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(timeUs / 1_000L)
        exoPlayer.playWhenReady = true
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            exoPlayer.seekTo(timeUs / 1000L)
        }
        exoPlayer.playWhenReady = isActive
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    controllerAutoShow = true
                    controllerShowTimeoutMs = 1500
                    //setOnClickListener { onToggleFullScreen() }
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        if (visibility == android.view.View.VISIBLE) {
                            val timeBar = findViewById<DefaultTimeBar>(R.id.exo_progress)
                            if (timeBar != null) {
                                val markerTimes = keyframes.map { it.timeUs / 1000 }.toLongArray()
                                timeBar.setAdGroupTimesMs(
                                    markerTimes,
                                    BooleanArray(markerTimes.size),
                                    markerTimes.size
                                )
                                timeBar.setAdMarkerColor(Color.RED)
                            }
                        }
                    })
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                if (isActive) {
                    view.showController()
                }
                val timeBar = view.findViewById<DefaultTimeBar>(R.id.exo_progress)
                if (timeBar != null) {
                    val markerTimes = keyframes.map { it.timeUs / 1000 }.toLongArray()
                    timeBar.setAdGroupTimesMs(
                        markerTimes,
                        BooleanArray(markerTimes.size),
                        markerTimes.size
                    )
                    timeBar.setAdMarkerColor(Color.RED)
                }
            }
        )
        androidx.compose.material3.IconButton(
            onClick = { onToggleFullScreen() },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomStart)
                .padding(8.dp)
                .background(
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        ) {
            androidx.compose.material3.Icon(
                imageVector = if (isFullScreen) {
                    androidx.compose.material.icons.Icons.Default.FullscreenExit
                } else {
                    androidx.compose.material.icons.Icons.Default.Fullscreen
                },
                contentDescription = "Toggle Fullscreen",
                tint = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}