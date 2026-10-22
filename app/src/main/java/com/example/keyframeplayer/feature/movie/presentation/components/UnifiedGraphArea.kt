package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.core.domain.model.toJapaneseClassName
import com.example.keyframeplayer.feature.movie.presentation.MovieUiState
import com.example.keyframeplayer.feature.movie.presentation.TimeScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

/**
 * =========================================================================================
 * 【統合グラフ (Unified Graph Area) - v1/v2 復元シームレス連続スクロール版】
 * =========================================================================================
 * 
 * - ページ分割 (HorizontalPager) によるページスナップ動作を廃止し、.horizontalScroll(scrollState)
 *   による1枚のシームレスな長尺タイムラインキャンバスとして描画します。
 * - 表示時間スケール (TimeScale: 1日 / 6時間 / 1.5時間) の1画面幅に対して、動画全体の時間幅
 *   (totalRealDurationMs) に比例した全長 (totalWidth) を動的に算定します。
 * - 再生中は赤バーが画面中央付近に来るように scrollState が滑らかに追従スクロールします。
 * =========================================================================================
 */
@Composable
fun UnifiedGraphArea(
    uiState: MovieUiState,
    onProgressTapped: (Float) -> Unit,
    onTimeScaleSelected: (TimeScale) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUiState by rememberUpdatedState(uiState)
    val currentOnProgressTapped by rememberUpdatedState(onProgressTapped)
    val coroutineScope = rememberCoroutineScope()

    val timeScale = uiState.selectedTimeScale
    val minReal = uiState.minRealTimeMs
    val scaleWindowMs = (timeScale.totalSeconds * 1000f).toLong().coerceAtLeast(1000L)
    val spanMs = maxOf(uiState.totalRealDurationMs, scaleWindowMs)

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // 赤バーのドラッグ状態
    var isDraggingRedBar by remember { mutableStateOf(false) }
    var localDragX by remember { mutableFloatStateOf(-1f) }

    // 再生位置が変更された場合に scrollState を中央付近へ自動スクロール（手動スクロール/ドラッグ中を除く）
    val currentRealTimeMs = uiState.currentRealTimeMs
    val currentProgress = if (spanMs > 0L) {
        ((currentRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LaunchedEffect(currentRealTimeMs, timeScale) {
        if (!scrollState.isScrollInProgress && !isDraggingRedBar) {
            val totalWidthPx = scrollState.maxValue + scrollState.viewportSize
            if (totalWidthPx > 0 && spanMs > 0L) {
                val targetScroll = (currentProgress * totalWidthPx) - (scrollState.viewportSize / 2f)
                scrollState.scrollTo(targetScroll.toInt().coerceIn(0, scrollState.maxValue))
            }
        }
    }

    // 赤バーを端付近でドラッグ維持している場合の背景（グラフ）自動スライド処理
    LaunchedEffect(isDraggingRedBar, localDragX) {
        if (isDraggingRedBar && localDragX >= 0f) {
            val edgeMarginPx = with(density) { 45.dp.toPx() }
            val scrollSpeedPx = with(density) { 14.dp.toPx() }

            while (isActive && isDraggingRedBar) {
                val currentScroll = scrollState.value.toFloat()
                val viewportWidth = scrollState.viewportSize.toFloat()

                if (viewportWidth > 0f) {
                    val screenX = localDragX - currentScroll

                    var delta = 0f
                    if (screenX < edgeMarginPx && currentScroll > 0) {
                        val speedFactor = ((edgeMarginPx - screenX) / edgeMarginPx).coerceIn(0.2f, 1f)
                        delta = -scrollSpeedPx * speedFactor
                    } else if (screenX > viewportWidth - edgeMarginPx && currentScroll < scrollState.maxValue) {
                        val speedFactor = ((screenX - (viewportWidth - edgeMarginPx)) / edgeMarginPx).coerceIn(0.2f, 1f)
                        delta = scrollSpeedPx * speedFactor
                    }

                    if (delta != 0f) {
                        val oldScroll = scrollState.value
                        scrollState.scrollBy(delta)
                        val actualDelta = scrollState.value - oldScroll
                        localDragX = (localDragX + actualDelta).coerceIn(0f, scrollState.maxValue.toFloat() + viewportWidth)
                    }
                }

                delay(16L)
            }
        }
    }

    // ピンチジェスチャーの累積倍率トラッキング
    var accumulatedZoom by remember { mutableFloatStateOf(1.0f) }

    // スケールサイズドロップダウンの開閉状態
    var timeScaleMenuExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .systemGestureExclusion()
            .background(MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium)
            .pointerInput(timeScale) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val changes = event.changes
                        if (changes.size >= 2) {
                            val p1 = changes[0]
                            val p2 = changes[1]
                            val currentDist = (p1.position - p2.position).getDistance()
                            val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()

                            if (prevDist > 0f && currentDist > 0f) {
                                val zoom = currentDist / prevDist
                                accumulatedZoom *= zoom
                                if (accumulatedZoom > 1.25f) {
                                    val newScale = timeScale.zoomIn()
                                    if (newScale != timeScale) {
                                        onTimeScaleSelected(newScale)
                                        accumulatedZoom = 1.0f
                                    }
                                } else if (accumulatedZoom < 0.75f) {
                                    val newScale = timeScale.zoomOut()
                                    if (newScale != timeScale) {
                                        onTimeScaleSelected(newScale)
                                        accumulatedZoom = 1.0f
                                    }
                                }
                                p1.consume()
                                p2.consume()
                            }
                        }
                    }
                }
            }
    ) {
        val parentMaxWidth = this.maxWidth
        val viewportWidthPx = constraints.maxWidth.toFloat()

        // 表示スケール (scaleWindowMs) に対する全時間軸 (spanMs) の倍率から総横幅を算出
        // Compose Constraints の上限値オーバーフロー (65,535dp / 262,143px) によるクラッシュを防止するため上限 (3600.dp) にクランプ
        val scaleRatio = spanMs.toFloat() / scaleWindowMs.toFloat()
        val maxAllowedWidth = 3600.dp
        val rawTotalWidth = parentMaxWidth * scaleRatio
        val totalWidth = if (rawTotalWidth > maxAllowedWidth) maxAllowedWidth else rawTotalWidth

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // ヘッダー: スケールサイズドロップダウン & ズーム操作ヒント
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // スケールサイズ切り替えドロップダウン
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { timeScaleMenuExpanded = true },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "${timeScale.label}表示",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "表示スケール選択",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = timeScaleMenuExpanded,
                            onDismissRequest = { timeScaleMenuExpanded = false }
                        ) {
                            TimeScale.entries.forEach { scale ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${scale.label}表示",
                                            fontWeight = if (scale == timeScale) FontWeight.Bold else FontWeight.Normal,
                                            color = if (scale == timeScale) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onTimeScaleSelected(scale)
                                        timeScaleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "🔍 ピンチで拡大縮小",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 一続きのシームレスな横スクロールコンテナ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .width(totalWidth)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val noVideoBgColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    val activeVideoBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    val activeBorderColor = primaryColor

                    // 1. ヒストグラム・赤バー描画用 Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                            .padding(horizontal = 16.dp)
                            .pointerInput(spanMs, minReal) {
                                val touchRadiusPx = 40.dp.toPx()

                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val canvasWidth = size.width.toFloat()
                                        if (canvasWidth <= 0f) continue

                                        fun updateProgressForX(x: Float) {
                                            val progress = (x / canvasWidth).coerceIn(0f, 1f)
                                            currentOnProgressTapped(progress)
                                        }

                                        val currentMs = currentUiState.currentRealTimeMs
                                        val cProgress = if (spanMs > 0L) {
                                            ((currentMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                        } else 0f
                                        val indicatorX = if (isDraggingRedBar && localDragX >= 0f) {
                                            localDragX
                                        } else {
                                            cProgress * canvasWidth
                                        }

                                        val isNearRedBar = kotlin.math.abs(down.position.x - indicatorX) <= touchRadiusPx

                                        var isLongPressConfirmed = false
                                        var currentPointerX = down.position.x
                                        val pointerId = down.id

                                        val longPressJob = coroutineScope.launch {
                                            if (isNearRedBar) {
                                                isLongPressConfirmed = true
                                                isDraggingRedBar = true
                                                localDragX = currentPointerX
                                            } else {
                                                delay(350L)
                                                isLongPressConfirmed = true
                                                isDraggingRedBar = true
                                                localDragX = currentPointerX
                                            }
                                        }

                                        var isSlidWithoutLongPress = false

                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Main)
                                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                                                currentPointerX = change.position.x

                                                if (!change.pressed) {
                                                    longPressJob.cancel()
                                                    if (isLongPressConfirmed || isDraggingRedBar) {
                                                        updateProgressForX(currentPointerX)
                                                    } else if (!isSlidWithoutLongPress) {
                                                        change.consume()
                                                        updateProgressForX(currentPointerX)
                                                    }
                                                    break
                                                }

                                                val slop = viewConfiguration.touchSlop
                                                val totalDistance = (change.position - down.position).getDistance()

                                                if (isLongPressConfirmed || isDraggingRedBar) {
                                                    longPressJob.cancel()
                                                    change.consume()
                                                    localDragX = currentPointerX
                                                } else if (totalDistance > slop) {
                                                    longPressJob.cancel()
                                                    isSlidWithoutLongPress = true
                                                    break
                                                }
                                            }
                                        } finally {
                                            longPressJob.cancel()
                                            isDraggingRedBar = false
                                            localDragX = -1f
                                        }
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // 1. 全体背景 (動画が存在しない背景区間)
                        drawRoundRect(
                            color = noVideoBgColor,
                            size = Size(canvasWidth, canvasHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // 2. 動画が存在する範囲 (ハイライト領域)
                        for (video in uiState.videos) {
                            val vStart = video.realStartTime
                            val vEnd = video.realEndTime
                            if (vEnd >= minReal && vStart <= minReal + spanMs) {
                                val startProgress = ((vStart - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                val endProgress = ((vEnd - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                val left = startProgress * canvasWidth
                                val right = endProgress * canvasWidth
                                val width = (right - left).coerceAtLeast(2f)

                                drawRect(
                                    color = activeVideoBgColor,
                                    topLeft = Offset(left, 0f),
                                    size = Size(width, canvasHeight)
                                )

                                drawLine(
                                    color = activeBorderColor,
                                    start = Offset(left, 0f),
                                    end = Offset(left, canvasHeight),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawLine(
                                    color = activeBorderColor,
                                    start = Offset(right, 0f),
                                    end = Offset(right, canvasHeight),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        }

                        // 3. 集計データバーの描画 (可視領域のみを描画するパフォーマンス最適化)
                        val barCounts = uiState.graphBarCounts
                        val totalBarCount = barCounts.size
                        if (totalBarCount > 0) {
                            val scrollOffset = scrollState.value.toFloat()
                            val spacing = 0.5.dp.toPx()
                            val barWidth = ((canvasWidth - (spacing * (totalBarCount + 1))) / totalBarCount).coerceAtLeast(1f)
                            val stepWidth = barWidth + spacing

                            val firstVisibleIndex = floor((scrollOffset - 16.dp.toPx()) / stepWidth).toInt().coerceAtLeast(0)
                            val lastVisibleIndex = ceil((scrollOffset + viewportWidthPx) / stepWidth).toInt().coerceAtMost(totalBarCount - 1)

                            val maxCount = uiState.maxBarCount.toFloat().coerceAtLeast(1f)
                            val cProgress = if (spanMs > 0L) {
                                ((currentRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            val currentBarIndex = (cProgress * totalBarCount).toInt().coerceIn(0, totalBarCount - 1)

                            for (i in firstVisibleIndex..lastVisibleIndex) {
                                val count = barCounts.getOrNull(i) ?: 0
                                if (count <= 0) continue

                                val left = spacing + (i * stepWidth)
                                val barHeightRatio = count.toFloat() / maxCount
                                val barHeightPx = (canvasHeight * 0.75f) * barHeightRatio
                                val top = canvasHeight - barHeightPx

                                val isCurrent = (i == currentBarIndex)
                                val color = if (isCurrent) Color.Red else primaryColor

                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, barHeightPx),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }

                        // 4. 赤バー（インジケーター）描画
                        val cProgress = if (spanMs > 0L) {
                            ((currentRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        val activeIndicatorX = if (isDraggingRedBar && localDragX >= 0f) {
                            localDragX
                        } else {
                            cProgress * canvasWidth
                        }

                        val lineStrokeWidth = if (isDraggingRedBar) 4.dp.toPx() else 2.5.dp.toPx()

                        drawLine(
                            color = Color.Red,
                            start = Offset(activeIndicatorX, 0f),
                            end = Offset(activeIndicatorX, canvasHeight),
                            strokeWidth = lineStrokeWidth
                        )

                        val displayMs = if (isDraggingRedBar && localDragX >= 0f) {
                            val ratio = (localDragX / canvasWidth).coerceIn(0f, 1f)
                            minReal + (ratio * spanMs).toLong()
                        } else {
                            currentRealTimeMs
                        }

                        val isAtLeastOneAndHalfHours = scaleWindowMs >= 5400 * 1000L
                        val dateTimeSdf = if (isAtLeastOneAndHalfHours) {
                            SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
                        } else {
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        }
                        val dateTimeText = if (displayMs > 0L) dateTimeSdf.format(Date(displayMs)) else ""

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
                            val bgLeft = (activeIndicatorX - bgWidth / 2f).coerceIn(0f, canvasWidth - bgWidth)

                            drawRoundRect(
                                color = if (isDraggingRedBar) Color(0xFFFF3333) else Color.Red,
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

                    // 2. スライド追従・動的タイム目盛り
                    UnifiedTimeTicks(
                        uiState = uiState,
                        scrollOffset = scrollState.value.toFloat(),
                        viewportWidthPx = viewportWidthPx
                    )
                }
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
    val tickTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val tickLineColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            val canvasWidth = size.width
            val minReal = uiState.minRealTimeMs
            val scaleWindowMs = (uiState.selectedTimeScale.totalSeconds * 1000f).toLong().coerceAtLeast(1000L)
            val spanMs = maxOf(uiState.totalRealDurationMs, scaleWindowMs)
            val maxReal = minReal + spanMs

            if (canvasWidth <= 0f || spanMs <= 0L) return@Canvas

            val minSpacingPx = 70.dp.toPx()
            val msPerPx = spanMs.toDouble() / canvasWidth.toDouble()
            val minIntervalMs = (minSpacingPx * msPerPx).toLong()

            val candidateIntervalsMs = listOf(
                10 * 1000L,           // 10秒
                30 * 1000L,           // 30秒
                1 * 60 * 1000L,       // 1分
                2 * 60 * 1000L,       // 2分
                5 * 60 * 1000L,       // 5分
                15 * 60 * 1000L,      // 15分
                30 * 60 * 1000L,      // 30分
                60 * 60 * 1000L,      // 1時間
                2 * 60 * 60 * 1000L,  // 2時間
                4 * 60 * 60 * 1000L,  // 4時間
                6 * 60 * 60 * 1000L,  // 6時間
                12 * 60 * 60 * 1000L, // 12時間
                24 * 60 * 60 * 1000L  // 24時間
            )
            val intervalMs = candidateIntervalsMs.firstOrNull { it >= minIntervalMs }
                ?: candidateIntervalsMs.last()

            val visibleStartMs = minReal + ((scrollOffset / canvasWidth) * spanMs).toLong()
            val visibleEndMs = minReal + (((scrollOffset + viewportWidthPx) / canvasWidth) * spanMs).toLong()

            val firstTick = (visibleStartMs / intervalMs) * intervalMs
            val lastTick = ((visibleEndMs / intervalMs) + 1) * intervalMs

            val secTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val minTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

            val isAtLeastOneAndHalfHours = scaleWindowMs >= 5400 * 1000L

            var tickTimeMs = firstTick
            var lastDrawnX = -1000f

            while (tickTimeMs <= lastTick && tickTimeMs <= maxReal + intervalMs) {
                if (tickTimeMs >= minReal) {
                    val progress = ((tickTimeMs - minReal).toDouble() / spanMs.toDouble()).coerceIn(0.0, 1.0)
                    val x = (progress * canvasWidth).toFloat()

                    if (x - lastDrawnX >= 55.dp.toPx() || lastDrawnX < 0f) {
                        lastDrawnX = x

                        drawLine(
                            color = tickLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, 5.dp.toPx()),
                            strokeWidth = 1.2.dp.toPx()
                        )

                        val label = when {
                            isAtLeastOneAndHalfHours -> dateTimeFormat.format(Date(tickTimeMs))
                            scaleWindowMs <= 10 * 60 * 1000L -> secTimeFormat.format(Date(tickTimeMs))
                            else -> minTimeFormat.format(Date(tickTimeMs))
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            size.height - 2.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = tickTextColor
                                textSize = 10.dp.toPx()
                                isAntiAlias = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
                tickTimeMs += intervalMs
            }
        }
    }
}

/**
 * クラス/色フィルター操作用コントロールUI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphControlArea(
    uiState: MovieUiState,
    onTimeScaleSelected: (TimeScale) -> Unit = {},
    onClassFilterSelected: (String?) -> Unit,
    onColorFilterSelected: (ImageColor?) -> Unit,
    onLikedFilterToggled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var classMenuExpanded by remember { mutableStateOf(false) }
    var colorMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // フィルター（クラス & 色 & いいね）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // クラスフィルター
            ExposedDropdownMenuBox(
                expanded = classMenuExpanded,
                onExpandedChange = { classMenuExpanded = !classMenuExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = uiState.selectedClassFilter?.toJapaneseClassName() ?: "すべて",
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
                        text = { Text("全てのクラス") },
                        onClick = {
                            onClassFilterSelected(null)
                            classMenuExpanded = false
                        }
                    )
                    uiState.availableClasses.forEach { className ->
                        DropdownMenuItem(
                            text = { Text(className.toJapaneseClassName()) },
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
                    value = uiState.selectedColorFilter?.displayName ?: "すべて",
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
                            text = { Text(color.displayName) },
                            onClick = {
                                onColorFilterSelected(color)
                                colorMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // ❤️ いいねトグルフィルターボタン
            FilterChip(
                selected = uiState.isLikedFilterOnly,
                onClick = onLikedFilterToggled,
                label = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite Filter",
                        tint = if (uiState.isLikedFilterOnly) Color(0xFFE53935) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}
