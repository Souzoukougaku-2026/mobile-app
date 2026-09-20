package com.example.keyframeplayer.ui.movie.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.ui.movie.MovieUiState
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun OverallScrollableArea(
    uiState: MovieUiState,
    scrollState: ScrollState,
    onProgressTapped: (Float) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0xFFF5F5F5))
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        // 常に「1画面＝6時間」のスケールで全体の幅を決定する
        val totalWidth = maxWidth * uiState.overallContentScale
        
        Box(
            modifier = Modifier.fillMaxSize().horizontalScroll(scrollState),
            contentAlignment = Alignment.CenterStart // 左詰め
        ) {
            Column(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onProgressTapped(offset.x / size.width)
                            }
                        },
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // 背景の目盛り (1時間ごと)
                    val hourCount = (uiState.totalDurationSeconds / 3600).toInt()
                    for (i in 0..hourCount) {
                        val x = (i.toFloat() * 3600 / uiState.totalDurationSeconds) * canvasWidth
                        drawLine(Color.LightGray, Offset(x, 0f), Offset(x, canvasHeight), 1.dp.toPx())
                    }

                    // 詳細グラフの枠をハイライト (1時間の枠)
                    val detailStart = uiState.detailedScrollStart * canvasWidth
                    val detailWidth = uiState.detailHighlightWidthRatio * canvasWidth
                    drawRect(
                        color = Color.Yellow.copy(alpha = 0.3f),
                        topLeft = Offset(detailStart, 0f),
                        size = Size(detailWidth, canvasHeight),
                    )

                    val barCount = uiState.barValues.size
                    if (barCount > 0) {
                        val spacing = 1.dp.toPx()
                        val barWidth = (canvasWidth - (spacing * (barCount + 1))) / barCount
                        val stepWidth = barWidth + spacing

                        val scrollOffset = scrollState.value.toFloat()
                        val firstIdx = floor((scrollOffset - 16.dp.toPx()) / stepWidth).toInt().coerceAtLeast(0)
                        val lastIdx = ceil((scrollOffset + viewportWidthPx) / stepWidth).toInt().coerceAtMost(barCount - 1)

                        for (index in firstIdx..lastIdx) {
                            val value = uiState.barValues[index]
                            val left = spacing + (index * stepWidth)
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

                OverallTimeTicks(uiState.totalDurationSeconds, totalWidth)
            }
        }
    }
}

@Composable
fun OverallTimeTicks(totalSeconds: Float, containerWidth: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.width(containerWidth).padding(horizontal = 16.dp)) {
        val hourCount = (totalSeconds / 3600).toInt()
        for (i in 0..hourCount step 2) {
            val bias = if (hourCount > 0) (i.toFloat() / (totalSeconds/3600)) * 2 - 1 else -1f
            Text(
                text = "${i}h",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterStart).offset(x = (i.toFloat() * 3600 / totalSeconds).let { 
                    // 簡易的な位置計算
                    containerWidth * it - 8.dp
                })
            )
        }
    }
}
