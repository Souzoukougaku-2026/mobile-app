package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.feature.movie.presentation.MovieUiState

@Composable
fun OverallGraphArea(
    uiState: MovieUiState,
    onProgressTapped: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp)
            .background(Color(0xFFF5F5F5))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onProgressTapped(offset.x / size.width)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            val tickCount = 4 
            for (i in 0..tickCount) {
                val x = (i.toFloat() / tickCount) * canvasWidth
                drawLine(Color.LightGray, Offset(x, 0f), Offset(x, canvasHeight), 1.dp.toPx())
            }

            val viewportStart = uiState.visibleRangeStart * canvasWidth
            val viewportWidth = uiState.visibleRangeWidth * canvasWidth
            drawRect(
                color = Color.Yellow.copy(alpha = 0.3f),
                topLeft = Offset(viewportStart, 0f),
                size = Size(viewportWidth, canvasHeight),
            )

            val barCount = uiState.barValues.size
            if (barCount > 0) {
                val spacing = 1.dp.toPx()
                val barWidth = (canvasWidth - (spacing * (barCount + 1))) / barCount
                uiState.barValues.forEachIndexed { index, value ->
                    val left = spacing + (index * (barWidth + spacing))
                    val top = canvasHeight * (1 - value.coerceIn(0f, 1f))
                    drawRect(
                        color = if (index == ((uiState.currentTime / uiState.totalDurationSeconds) * barCount).toInt().coerceIn(0, barCount-1)) Color.Red else Color.Blue.copy(0.3f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, canvasHeight - top)
                    )
                }
            }

            val indicatorX = (uiState.currentTime / uiState.totalDurationSeconds) * canvasWidth
            drawLine(Color.Red, Offset(indicatorX, 0f), Offset(indicatorX, canvasHeight), 2.dp.toPx())
        }
    }
}

@Composable
fun OverallTimeTicks(totalSeconds: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val tickCount = 4
        for (i in 0..tickCount) {
            val label = "${(i * (totalSeconds / 3600 / 4)).toInt()}h"
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
