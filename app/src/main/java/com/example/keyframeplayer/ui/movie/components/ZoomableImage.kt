package com.example.keyframeplayer.ui.movie.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImagePainter

@Composable
fun ZoomableImage(
    painter: Painter,
    initialTopLeft: Offset, // (x, y) 0-100, y=0 is bottom
    initialBottomRight: Offset,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    val imageSize = painter.intrinsicSize
    val isImageReady = imageSize.isSpecified && imageSize.width > 0

    // 画像がロードされた瞬間に一度だけ実行する初期配置
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(containerSize, imageSize, isImageReady) {
        if (isImageReady && containerSize.width > 0 && !isInitialized) {
            val targetW = (initialBottomRight.x - initialTopLeft.x).coerceAtLeast(1f) / 100f
            val targetH = (initialTopLeft.y - initialBottomRight.y).coerceAtLeast(1f) / 100f
            
            val sX = containerSize.width / (imageSize.width * targetW)
            val sY = containerSize.height / (imageSize.height * targetH)
            scale = minOf(sX, sY).coerceIn(0.1f, 50f)
            
            val centerX = (initialTopLeft.x + initialBottomRight.x) / 200f
            val centerY = (100f - (initialTopLeft.y + initialBottomRight.y) / 2f) / 100f
            
            offset = Offset(
                x = (containerSize.width / 2f) - (imageSize.width * scale * centerX),
                y = (containerSize.height / 2f) - (imageSize.height * scale * centerY)
            )
            isInitialized = true
        }
    }

    // パスが変わったときに再初期化を許可する
    LaunchedEffect(initialTopLeft, initialBottomRight, painter) {
        isInitialized = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black)
                .clipToBounds()
                .onGloballyPositioned { containerSize = it.size.toSize() }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (!isImageReady) return@detectTransformGestures
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.1f, 100f)
                        val newOffset = (offset - centroid) * (newScale / oldScale) + centroid + pan
                        scale = newScale
                        offset = newOffset
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 重要: 非表示のImageを置くことで Coil に強制的にロードを開始させる
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(1.dp),
                alpha = 0f // 透明にする
            )

            if (!isImageReady) {
                CircularProgressIndicator(color = Color.White)
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isImageReady) {
                    withTransform({
                        translate(offset.x, offset.y)
                        scale(scale, scale, Offset.Zero)
                    }) {
                        with(painter) {
                            draw(imageSize)
                        }

                        // BBox の描画 (赤枠)
                        val left = initialTopLeft.x / 100f * imageSize.width
                        val top = (100f - initialTopLeft.y) / 100f * imageSize.height
                        val rWidth = (initialBottomRight.x - initialTopLeft.x) / 100f * imageSize.width
                        val rHeight = (initialTopLeft.y - initialBottomRight.y) / 100f * imageSize.height
                        
                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(left, top),
                            size = Size(rWidth, rHeight),
                            style = Stroke(width = 2.dp.toPx() / scale)
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color.DarkGray.copy(alpha = 0.5f)),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                if (isImageReady) {
                    val oldScale = scale
                    scale = (scale * 1.2f).coerceAtMost(100f)
                    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                    offset = (offset - center) * (scale / oldScale) + center
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            IconButton(onClick = {
                if (isImageReady) {
                    val oldScale = scale
                    scale = (scale / 1.2f).coerceAtLeast(0.1f)
                    val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                    offset = (offset - center) * (scale / oldScale) + center
                }
            }) {
                Icon(Icons.Default.Clear, contentDescription = "Zoom Out", tint = Color.White)
            }
        }
    }
}
