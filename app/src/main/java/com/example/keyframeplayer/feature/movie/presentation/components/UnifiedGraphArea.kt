package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.feature.movie.presentation.MovieUiState
import com.example.keyframeplayer.feature.movie.presentation.TimeScale

@Composable
fun UnifiedGraphArea(
    uiState: MovieUiState,
    onProgressTapped: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 横幅可変スライド（スクロール）対応 Box
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
        ) {
            val barCounts = uiState.graphBarCounts
            val totalBarCount = barCounts.size
            val windowBarCount = uiState.selectedTimeScale.barCount // 1画面分あたりの標準バー本数 (90本)

            val parentMaxWidth = this.maxWidth

            // バー全体の総幅を算出（スクロール領域）
            val totalContentWidth = if (windowBarCount > 0 && totalBarCount > 0) {
                parentMaxWidth * (totalBarCount.toFloat() / windowBarCount)
            } else {
                parentMaxWidth
            }

            Box(
                modifier = Modifier
                    .width(totalContentWidth)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (size.width > 0) {
                                val progress = (offset.x / size.width).coerceIn(0f, 1f)
                                onProgressTapped(progress)
                            }
                        }
                    }
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 目盛りグリッド線
                    val intervalMinutes = uiState.selectedTimeScale.intervalMinutes
                    val totalMinutes = totalBarCount * intervalMinutes
                    val ticksCount = (totalMinutes / 60).coerceAtLeast(1)

                    for (i in 0..ticksCount) {
                        val x = if (ticksCount > 0) (i.toFloat() / ticksCount) * canvasWidth else 0f
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 棒グラフの描画
                    val maxCount = uiState.maxBarCount.toFloat()
                    val baseRealMs = uiState.graphBaseRealTimeMs
                    val videoTotalMs = (uiState.totalDurationSeconds * 1000f).toLong().coerceAtLeast(1L)
                    val totalSpanMs = maxOf(86400000L, videoTotalMs)
                    val currentRealMs = uiState.currentRealTimeMs

                    val currentProgress = if (totalSpanMs > 0L && currentRealMs >= baseRealMs) {
                        ((currentRealMs - baseRealMs).toFloat() / totalSpanMs).coerceIn(0f, 1f)
                    } else 0f

                    if (totalBarCount > 0) {
                        val spacing = 1.dp.toPx()
                        val barWidth = (canvasWidth - (spacing * (totalBarCount + 1))) / totalBarCount
                        val currentBarIndex = (currentProgress * totalBarCount).toInt().coerceIn(0, totalBarCount - 1)

                        barCounts.forEachIndexed { index, count ->
                            val left = spacing + index * (barWidth + spacing)
                            val barHeightRatio = count.toFloat() / maxCount
                            val barHeightPx = canvasHeight * barHeightRatio.coerceIn(0f, 1f)
                            val top = canvasHeight - barHeightPx

                            val isCurrent = (index == currentBarIndex)
                            val color = if (isCurrent) {
                                Color.Red
                            } else if (count > 0) {
                                primaryColor
                            } else {
                                Color.Transparent
                            }

                            if (count > 0) {
                                drawRect(
                                    color = color,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barHeightPx)
                                )
                            }
                        }
                    }

                    // 現在位置インジケーター (赤色の垂直線)
                    val indicatorX = currentProgress * canvasWidth

                    drawLine(
                        color = Color.Red,
                        start = Offset(indicatorX, 0f),
                        end = Offset(indicatorX, canvasHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
    }
}

/**
 * 時間スケール切替タブ & クラス/色フィルター操作用コントロールUI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphControlArea(
    uiState: MovieUiState,
    onTimeScaleSelected: (TimeScale) -> Unit,
    onClassFilterSelected: (String?) -> Unit,
    onColorFilterSelected: (ImageColor?) -> Unit,
    modifier: Modifier = Modifier
) {
    var classMenuExpanded by remember { mutableStateOf(false) }
    var colorMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. 時間スケール切り替えボタン群 (1日 / 6時間 / 1.5時間)
        Text(
            text = "表示時間スケール",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeScale.entries.forEach { scale ->
                val isSelected = uiState.selectedTimeScale == scale
                FilterChip(
                    selected = isSelected,
                    onClick = { onTimeScaleSelected(scale) },
                    label = {
                        Text("${scale.label} (${scale.intervalMinutes}分刻み)")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. フィルター（クラス & 色）のドロップダウンメニュー
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // クラスフィルター
            ExposedDropdownMenuBox(
                expanded = classMenuExpanded,
                onExpandedChange = { classMenuExpanded = !classMenuExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.selectedClassFilter ?: "すべてのクラス",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("クラス絞り込み") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = classMenuExpanded,
                    onDismissRequest = { classMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("すべてのクラス") },
                        onClick = {
                            onClassFilterSelected(null)
                            classMenuExpanded = false
                        }
                    )
                    uiState.availableClasses.forEach { className ->
                        DropdownMenuItem(
                            text = { Text(className) },
                            onClick = {
                                onClassFilterSelected(className)
                                classMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 色フィルター
            ExposedDropdownMenuBox(
                expanded = colorMenuExpanded,
                onExpandedChange = { colorMenuExpanded = !colorMenuExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.selectedColorFilter?.name ?: "すべての色",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("色絞り込み") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = colorMenuExpanded,
                    onDismissRequest = { colorMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("すべての色") },
                        onClick = {
                            onColorFilterSelected(null)
                            colorMenuExpanded = false
                        }
                    )
                    uiState.availableColors.forEach { color ->
                        DropdownMenuItem(
                            text = { Text(color.name) },
                            onClick = {
                                onColorFilterSelected(color)
                                colorMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
