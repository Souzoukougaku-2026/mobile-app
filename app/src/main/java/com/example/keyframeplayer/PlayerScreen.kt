package com.example.keyframeplayer

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun PlayerScreen(
    viewModel: SharedViewModel,
    timeUs: Long
) {
    val context = LocalContext.current
    val uri by viewModel.selectedUri.collectAsState()

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    LaunchedEffect(uri) {
        val currentUri = uri ?: return@LaunchedEffect
        val mediaItem = MediaItem.fromUri(currentUri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(timeUs / 1_000L)
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        }
    )
}