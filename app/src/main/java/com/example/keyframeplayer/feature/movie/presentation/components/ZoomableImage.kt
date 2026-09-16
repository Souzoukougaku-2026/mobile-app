package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun ZoomableImage(
    painter: Painter? = null,
    initialTopLeft: Offset, // (x, y) 0-100, y=0 is bottom (unless isPixelCoordinates is true)
    initialBottomRight: Offset,
    imagePath: String? = null,
    isPixelCoordinates: Boolean = false,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }
    
    val currentPainter = if (imagePath != null) {
        coil.compose.rememberAsyncImagePainter(imagePath)
    } else {
        painter
    }
    
    val imageSize = currentPainter?.intrinsicSize ?: Size.Unspecified

    // 初期配置計算
    LaunchedEffect(containerSize, imageSize) {
        if (containerSize == Size.Zero || imageSize == Size.Unspecified) return@LaunchedEffect
        
        val targetW = if (isPixelCoordinates) {
            (initialBottomRight.x - initialTopLeft.x) / imageSize.width
        } else {
            (initialBottomRight.x - initialTopLeft.x) / 100f
        }
        val targetH = if (isPixelCoordinates) {
            (initialBottomRight.y - initialTopLeft.y) / imageSize.height
        } else {
            (initialTopLeft.y - initialBottomRight.y) / 100f
        }
        
        val sX = containerSize.width / (imageSize.width * targetW)
        val sY = containerSize.height / (imageSize.height * targetH)
        scale = minOf(sX, sY)
        
        val centerX = if (isPixelCoordinates) {
            ((initialTopLeft.x + initialBottomRight.x) / 2f) / imageSize.width
        } else {
            (initialTopLeft.x + initialBottomRight.x) / 200f
        }
        val centerY = if (isPixelCoordinates) {
            ((initialTopLeft.y + initialBottomRight.y) / 2f) / imageSize.height
        } else {
            (100f - (initialTopLeft.y + initialBottomRight.y) / 2f) / 100f
        }
        
        offset = Offset(
            x = (containerSize.width / 2f) - (imageSize.width * scale * centerX),
            y = (containerSize.height / 2f) - (imageSize.height * scale * centerY)
        )
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
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.1f, 50f)
                        
                        val newOffset = (offset - centroid) * (newScale / oldScale) + centroid + pan
                        
                        val sw = imageSize.width * newScale
                        val sh = imageSize.height * newScale
                        val cx = containerSize.width / 2f
                        val cy = containerSize.height / 2f
                        
                        val bx = newOffset.x.coerceIn(cx - sw, cx)
                        val by = newOffset.y.coerceIn(cy - sh, cy)
                        
                        scale = newScale
                        offset = Offset(bx, by)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (imageSize != Size.Unspecified && currentPainter != null) {
                    withTransform({
                        translate(offset.x, offset.y)
                        scale(scale, scale, Offset.Zero)
                    }) {
                        with(currentPainter) {
                            draw(imageSize)
                        }

                        val left = if (isPixelCoordinates) initialTopLeft.x else initialTopLeft.x / 100f * imageSize.width
                        val top = if (isPixelCoordinates) initialTopLeft.y else (100f - initialTopLeft.y) / 100f * imageSize.height
                        val rWidth = if (isPixelCoordinates) {
                            initialBottomRight.x - initialTopLeft.x
                        } else {
                            (initialBottomRight.x - initialTopLeft.x) / 100f * imageSize.width
                        }
                        val rHeight = if (isPixelCoordinates) {
                            initialBottomRight.y - initialTopLeft.y
                        } else {
                            (initialTopLeft.y - initialBottomRight.y) / 100f * imageSize.height
                        }
                        
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
                val oldScale = scale
                val newScale = (scale * 1.2f).coerceIn(0.1f, 50f)
                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                val newOffset = (offset - center) * (newScale / oldScale) + center
                val sw = imageSize.width * newScale
                val sh = imageSize.height * newScale
                val cx = containerSize.width / 2f
                val cy = containerSize.height / 2f
                scale = newScale
                offset = Offset(newOffset.x.coerceIn(cx - sw, cx), newOffset.y.coerceIn(cy - sh, cy))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            IconButton(onClick = {
                val oldScale = scale
                val newScale = (scale / 1.2f).coerceIn(0.1f, 50f)
                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                val newOffset = (offset - center) * (newScale / oldScale) + center
                val sw = imageSize.width * newScale
                val sh = imageSize.height * newScale
                val cx = containerSize.width / 2f
                val cy = containerSize.height / 2f
                scale = newScale
                offset = Offset(newOffset.x.coerceIn(cx - sw, cx), newOffset.y.coerceIn(cy - sh, cy))
            }) {
                Icon(Icons.Default.Clear, contentDescription = "Zoom Out", tint = Color.White)
            }
        }
    }
}