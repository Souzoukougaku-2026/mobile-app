package com.example.a2026souzou.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.a2026souzou.feature.movie.presentation.MovieUiState
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun DetailedScrollableArea(
    uiState: MovieUiState,
    scrollState: ScrollState,
    onTimeChanged: (Float) -> Unit,
    onProgressTapped: (Float) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color(0xFFEEEEEE))
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val totalWidth = maxWidth / uiState.visibleRangeWidth
        
        Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
            Column(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onProgressTapped(offset.x / size.width)
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCount = uiState.detailedBarValues.size
                    if (barCount <= 0) return@Canvas

                    // 描画の最適化: 可視範囲のみループを回す
                    val scrollOffset = scrollState.value.toFloat()
                    val spacing = 1.dp.toPx()
                    val barWidth = (canvasWidth - (spacing * (barCount + 1))) / barCount
                    val stepWidth = barWidth + spacing

                    // 画面内に見えるインデックスの範囲を計算
                    val firstVisibleIndex = floor((scrollOffset - 16.dp.toPx()) / stepWidth).toInt().coerceAtLeast(0)
                    val lastVisibleIndex = ceil((scrollOffset + viewportWidthPx) / stepWidth).toInt().coerceAtMost(barCount - 1)

                    // 目盛り線 (可視範囲のみ)
                    val totalMinutes = (uiState.totalDurationSeconds / 60).toInt()
                    val tickIntervalMinutes = 15
                    val pixelsPerMinute = canvasWidth / totalMinutes
                    
                    val firstVisibleMinute = floor(scrollOffset / pixelsPerMinute).toInt().coerceAtLeast(0)
                    val lastVisibleMinute = ceil((scrollOffset + viewportWidthPx) / pixelsPerMinute).toInt().coerceAtMost(totalMinutes)

                    for (m in firstVisibleMinute..lastVisibleMinute) {
                        if (m % tickIntervalMinutes == 0) {
                            val x = (m.toFloat() / totalMinutes) * canvasWidth
                            drawLine(Color.LightGray, Offset(x, 0f), Offset(x, canvasHeight), 1.dp.toPx())
                        }
                    }

                    // 棒グラフ (可視範囲のみ)
                    val currentBarIndex = ((uiState.currentTime / uiState.totalDurationSeconds) * barCount).toInt().coerceIn(0, barCount - 1)
                    
                    for (index in firstVisibleIndex..lastVisibleIndex) {
                        val value = uiState.detailedBarValues[index]
                        val left = spacing + (index * stepWidth)
                        val top = canvasHeight * (1 - value.coerceIn(0f, 1f))
                        val barColor = when {
                            index == currentBarIndex -> Color.Red
                            index < currentBarIndex -> Color.Blue.copy(alpha = 0.6f)
                            else -> Color.Blue.copy(alpha = 0.2f)
                        }
                        drawRect(barColor, Offset(left, top), Size(barWidth, canvasHeight - top))
                    }

                    // 現在地インジケータ
                    val indicatorX = (uiState.currentTime / uiState.totalDurationSeconds) * canvasWidth
                    drawLine(Color.Red, Offset(indicatorX, 0f), Offset(indicatorX, canvasHeight), 2.dp.toPx())
                }

                // 目盛りラベルも最適化が必要な可能性があるが、まずはグラフの描画を優先
                DetailedTimeTicks(uiState.totalDurationSeconds, scrollState.value.toFloat(), viewportWidthPx)

                SlimSlider(
                    value = uiState.currentTime,
                    onValueChange = onTimeChanged,
                    valueRange = 0f..uiState.totalDurationSeconds,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(20.dp)
                )
            }
        }
    }
}

@Composable
fun DetailedTimeTicks(totalSeconds: Float, scrollOffset: Float, viewportWidthPx: Float) {
    val totalMinutes = (totalSeconds / 60).toInt()
    val tickIntervalMinutes = 15
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 1.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(12.dp)) {
            val canvasWidth = size.width
            val pixelsPerMinute = canvasWidth / totalMinutes
            
            val firstVisibleMinute = floor(scrollOffset / pixelsPerMinute).toInt().coerceAtLeast(0)
            val lastVisibleMinute = ceil((scrollOffset + viewportWidthPx) / pixelsPerMinute).toInt().coerceAtMost(totalMinutes)

            for (m in firstVisibleMinute..lastVisibleMinute) {
                if (m % tickIntervalMinutes == 0) {
                    val x = (m.toFloat() / totalMinutes) * canvasWidth
                    val h = m / 60
                    val mm = m % 60
                    val label = "%02d:%02d".format(h, mm)
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        size.height,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 9.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}
