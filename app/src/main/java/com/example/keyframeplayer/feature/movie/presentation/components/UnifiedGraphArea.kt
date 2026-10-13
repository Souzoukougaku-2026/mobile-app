package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.feature.movie.presentation.MovieUiState
import com.example.keyframeplayer.feature.movie.presentation.TimeScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun UnifiedGraphArea(
    uiState: MovieUiState,
    scrollState: ScrollState,
    onProgressTapped: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val parentMaxWidth = this.maxWidth

        // movieDetail 応用: visibleRangeWidth に基づくスワイプ全幅設定
        val visibleWidthRatio = uiState.visibleRangeWidth.coerceIn(0.02f, 1f)
        val maxAllowedWidth = 4000.dp
        val calculatedWidth = parentMaxWidth / visibleWidthRatio
        val totalWidth = if (calculatedWidth > maxAllowedWidth) maxAllowedWidth else calculatedWidth

        // 時間スケール切り替え時に現在再生中の位置を中央へスクロール同期
        LaunchedEffect(uiState.selectedTimeScale) {
            val totalWidthPx = scrollState.maxValue + scrollState.viewportSize
            val spanMs = maxOf(uiState.totalRealDurationMs, 24 * 3600 * 1000L).toFloat()
            if (totalWidthPx > 0 && spanMs > 0f) {
                val currentProgress = ((uiState.currentRealTimeMs - uiState.minRealTimeMs).toFloat() / spanMs).coerceIn(0f, 1f)
                val targetScroll = (currentProgress * totalWidthPx) - (scrollState.viewportSize / 2f)
                scrollState.scrollTo(targetScroll.toInt().coerceIn(0, scrollState.maxValue))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .width(totalWidth)
                    .padding(vertical = 8.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary

                // 1. 統合ヒストグラム グラフ Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp)
                        .padding(horizontal = 16.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (size.width > 0) {
                                    onProgressTapped((offset.x / size.width).coerceIn(0f, 1f))
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barCounts = uiState.graphBarCounts
                    val barCount = barCounts.size
                    val minReal = uiState.minRealTimeMs
                    val spanMs = maxOf(uiState.totalRealDurationMs, 24 * 3600 * 1000L)
                    val totalDuration = spanMs.toFloat().coerceAtLeast(1.0f)

                    // 動画区間の背景ハイライト描画
                    val videoOverlayColor = primaryColor.copy(alpha = 0.12f)
                    for (video in uiState.videos) {
                        val startProgress = ((video.realStartTime - minReal).toFloat() / totalDuration).coerceIn(0f, 1f)
                        val endProgress = ((video.realEndTime - minReal).toFloat() / totalDuration).coerceIn(0f, 1f)
                        val left = startProgress * canvasWidth
                        val right = endProgress * canvasWidth
                        val width = (right - left).coerceAtLeast(2f)

                        drawRect(
                            color = videoOverlayColor,
                            topLeft = Offset(left, 0f),
                            size = Size(width, canvasHeight)
                        )
                    }

                    if (barCount > 0) {
                        val scrollOffset = scrollState.value.toFloat()
                        val spacing = 0.5.dp.toPx()
                        val barWidth = ((canvasWidth - (spacing * (barCount + 1))) / barCount).coerceAtLeast(1f)
                        val stepWidth = barWidth + spacing

                        val firstVisibleIndex = floor((scrollOffset - 16.dp.toPx()) / stepWidth).toInt().coerceAtLeast(0)
                        val lastVisibleIndex = ceil((scrollOffset + viewportWidthPx) / stepWidth).toInt().coerceAtMost(barCount - 1)

                        val maxCount = uiState.maxBarCount.toFloat().coerceAtLeast(1f)
                        val currentProgress = ((uiState.currentRealTimeMs - minReal).toFloat() / totalDuration).coerceIn(0f, 1f)
                        val currentBarIndex = (currentProgress * barCount).toInt().coerceIn(0, barCount - 1)

                        // 可視範囲の棒グラフを描画 (データ件数に素直に比例した高さで表示)
                        for (index in firstVisibleIndex..lastVisibleIndex) {
                            val count = barCounts[index]
                            if (count <= 0) continue

                            val left = spacing + (index * stepWidth)
                            val barHeightRatio = count.toFloat() / maxCount
                            val barHeightPx = (canvasHeight * 0.75f) * barHeightRatio
                            val top = canvasHeight - barHeightPx

                            val isCurrent = (index == currentBarIndex)
                            val color = if (isCurrent) Color.Red else primaryColor

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeightPx),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }

                        // 現在位置インジケーター (赤色垂直線 & 日時バブルマーカー)
                        val indicatorX = currentProgress * canvasWidth

                        drawLine(
                            color = Color.Red,
                            start = Offset(indicatorX, 0f),
                            end = Offset(indicatorX, canvasHeight),
                            strokeWidth = 2.5.dp.toPx()
                        )

                        // 日時を含むバブルマーカー (月/日 時:分:秒)
                        val dateTimeSdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
                        val dateTimeText = if (uiState.currentRealTimeMs > 0L) {
                            dateTimeSdf.format(Date(uiState.currentRealTimeMs))
                        } else {
                            ""
                        }

                        if (dateTimeText.isNotEmpty()) {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 10.dp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            val textWidth = paint.measureText(dateTimeText)
                            val bgWidth = textWidth + 14.dp.toPx()
                            val bgHeight = 20.dp.toPx()
                            val bgLeft = (indicatorX - bgWidth / 2f).coerceIn(0f, canvasWidth - bgWidth)

                            drawRoundRect(
                                color = Color.Red,
                                topLeft = Offset(bgLeft, 0f),
                                size = Size(bgWidth, bgHeight),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                dateTimeText,
                                bgLeft + bgWidth / 2f,
                                bgHeight - 5.dp.toPx(),
                                paint
                            )
                        }
                    }
                }

                // 2. スライド追従日時目盛り
                UnifiedTimeTicks(
                    uiState = uiState,
                    scrollOffset = scrollState.value.toFloat(),
                    viewportWidthPx = viewportWidthPx
                )
            }
        }
    }
}

/**
 * 画面横スライドに対応した動的「日時」目盛りラベル
 */
@Composable
fun UnifiedTimeTicks(
    uiState: MovieUiState,
    scrollOffset: Float,
    viewportWidthPx: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
        ) {
            val canvasWidth = size.width
            val minReal = uiState.minRealTimeMs
            val spanMs = maxOf(uiState.totalRealDurationMs, 24 * 3600 * 1000L)
            val maxReal = minReal + spanMs

            val intervalMs = when (uiState.selectedTimeScale) {
                TimeScale.ONE_HALF_HOURS -> 10 * 60 * 1000L // 10分刻み
                TimeScale.SIX_HOURS -> 30 * 60 * 1000L      // 30分刻み
                TimeScale.ONE_DAY -> 2 * 60 * 60 * 1000L    // 2時間刻み
            }

            val visibleStartMs = minReal + ((scrollOffset / canvasWidth) * spanMs).toLong()
            val visibleEndMs = minReal + (((scrollOffset + viewportWidthPx) / canvasWidth) * spanMs).toLong()

            val firstTick = (visibleStartMs / intervalMs) * intervalMs
            val lastTick = ((visibleEndMs / intervalMs) + 1) * intervalMs

            val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

            var tickTimeMs = firstTick
            while (tickTimeMs <= lastTick && tickTimeMs <= maxReal + intervalMs) {
                if (tickTimeMs >= minReal) {
                    val progress = ((tickTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                    val x = progress * canvasWidth

                    // 目盛り刻み線
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, 5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )

                    // 日時ラベル (MM/dd HH:mm)
                    val label = dateTimeFormat.format(Date(tickTimeMs))

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        size.height - 2.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 9.5.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
                tickTimeMs += intervalMs
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
                        Text(scale.label)
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

