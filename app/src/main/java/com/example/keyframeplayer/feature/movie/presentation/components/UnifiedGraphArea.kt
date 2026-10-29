package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.core.data.database.entity.VideoEntity
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
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.floor


/**
 * =========================================================================================
 * 【統合グラフ (Unified Graph Area) - 直感的にカスタマイズ可能な全スケール黄金比目盛り版】
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
    val scaleWindowMs = timeScale.windowMs
    val minReal = uiState.minRealTimeMs
    val spanMs = maxOf(uiState.totalRealDurationMs, scaleWindowMs)

    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // 赤バーのドラッグ状態
    var isDraggingRedBar by remember { mutableStateOf(false) }
    // 指の生の位置 (ドラッグ中の端付近オートスクロール判定に使用)
    var localDragX by remember { mutableFloatStateOf(-1f) }
    // 赤バーの表示位置 (動画が存在する区間の中だけに制限された位置)
    var dragBarX by remember { mutableFloatStateOf(-1f) }
    // グラフ Canvas の実幅 (オートスクロール中のバー位置再計算に使用)
    var graphCanvasWidthPx by remember { mutableFloatStateOf(0f) }

    // 再生位置が変更された場合に scrollState を中央付近へ自動スクロール
    val currentRealTimeMs = uiState.currentRealTimeMs
    val currentProgress = if (spanMs > 0L) {
        ((currentRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // 絞り込み適用済みのキーフレーム一覧 (時間昇順)
    val filteredKeyframes = remember(
        uiState.keyframes,
        uiState.selectedClassFilter,
        uiState.selectedColorFilter,
        uiState.isLikedFilterOnly
    ) {
        uiState.keyframes
            .filter { item ->
                (uiState.selectedClassFilter == null || item.className == uiState.selectedClassFilter) &&
                (uiState.selectedColorFilter == null || item.color == uiState.selectedColorFilter) &&
                (!uiState.isLikedFilterOnly || item.isLiked)
            }
            .sortedBy { it.realTime }
    }

    // 現在時刻より前の最新オブジェクト
    val prevKeyframe = remember(filteredKeyframes, currentRealTimeMs) {
        filteredKeyframes.filter { it.realTime < currentRealTimeMs - 500L }.maxByOrNull { it.realTime }
    }

    // 現在時刻より後の最古オブジェクト
    val nextKeyframe = remember(filteredKeyframes, currentRealTimeMs) {
        filteredKeyframes.filter { it.realTime > currentRealTimeMs + 500L }.minByOrNull { it.realTime }
    }

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
                        val wasBarFollowing = dragBarX >= 0f && dragBarX == localDragX
                        val oldScroll = scrollState.value
                        scrollState.scrollBy(delta)
                        val actualDelta = scrollState.value - oldScroll
                        localDragX = (localDragX + actualDelta).coerceIn(0f, scrollState.maxValue.toFloat() + viewportWidth)
                        // バーもスクロールに追随させる。ただし動画のない区間では端で止まったままにする
                        if (dragBarX >= 0f) {
                            dragBarX = if (graphCanvasWidthPx > 0f) {
                                snapDragBarXToVideoRange(
                                    videos = currentUiState.videos,
                                    minReal = minReal,
                                    spanMs = spanMs,
                                    canvasWidth = graphCanvasWidthPx,
                                    rawX = localDragX,
                                    lastBarX = dragBarX
                                )
                            } else if (wasBarFollowing) {
                                (dragBarX + actualDelta).coerceIn(0f, scrollState.maxValue.toFloat() + viewportWidth)
                            } else {
                                dragBarX
                            }
                        }
                    }
                }

                delay(16L)
            }
        }
    }

    // ピンチジェスチャーの累積倍率トラッキング
    var accumulatedZoom by remember { mutableFloatStateOf(1.0f) }

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
        // Compose の安全描画上限 (20,000.dp) を適用し、各スケールでの1画面表示時間幅のクランプ歪みを防止
        val scaleRatio = spanMs.toFloat() / scaleWindowMs.toFloat()
        val maxAllowedWidth = 20000.dp
        val rawTotalWidth = parentMaxWidth * scaleRatio
        val totalWidth = if (rawTotalWidth > maxAllowedWidth) maxAllowedWidth else rawTotalWidth

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // ヘッダー: スケールサイズドロップダウン & ズーム操作ヒント
            UnifiedGraphHeader(
                timeScale = timeScale,
                hasPrevKeyframe = prevKeyframe != null,
                hasNextKeyframe = nextKeyframe != null,
                onTimeScaleSelected = onTimeScaleSelected,
                onSeekToPrevKeyframe = {
                    prevKeyframe?.let { kf ->
                        if (spanMs > 0L) {
                            val progress = ((kf.realTime - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                            onProgressTapped(progress)
                        }
                    }
                },
                onSeekToNextKeyframe = {
                    nextKeyframe?.let { kf ->
                        if (spanMs > 0L) {
                            val progress = ((kf.realTime - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                            onProgressTapped(progress)
                        }
                    }
                }
            )

            // 一続きのシームレスな横スクロールコンテナ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier.width(totalWidth)
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    // 動画が存在しない背景部分: トーンを抑えたシックなベースカラー
                    val noVideoBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    // 動画が存在する領域: 鮮やかで際立つ強調ハイライトカラー
                    val activeVideoBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    val activeBorderColor = primaryColor
                    val activeTopLineColor = primaryColor

                    // 1. ヒストグラム・赤バー描画用 Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                            .padding(horizontal = 16.dp)
                            .onSizeChanged { graphCanvasWidthPx = it.width.toFloat() }
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
                                        val indicatorX = if (isDraggingRedBar && dragBarX >= 0f) {
                                            dragBarX
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
                                                dragBarX = snapDragBarXToVideoRange(
                                                    videos = currentUiState.videos,
                                                    minReal = minReal,
                                                    spanMs = spanMs,
                                                    canvasWidth = canvasWidth,
                                                    rawX = currentPointerX,
                                                    lastBarX = indicatorX
                                                )
                                            } else {
                                                delay(350L)
                                                isLongPressConfirmed = true
                                                isDraggingRedBar = true
                                                localDragX = currentPointerX
                                                dragBarX = snapDragBarXToVideoRange(
                                                    videos = currentUiState.videos,
                                                    minReal = minReal,
                                                    spanMs = spanMs,
                                                    canvasWidth = canvasWidth,
                                                    rawX = currentPointerX,
                                                    lastBarX = indicatorX
                                                )
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
                                                        // ドラッグ終了位置は、動画区間内に制限されたバーの位置を使う
                                                        updateProgressForX(if (dragBarX >= 0f) dragBarX else currentPointerX)
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
                                                    dragBarX = snapDragBarXToVideoRange(
                                                        videos = currentUiState.videos,
                                                        minReal = minReal,
                                                        spanMs = spanMs,
                                                        canvasWidth = canvasWidth,
                                                        rawX = currentPointerX,
                                                        lastBarX = dragBarX
                                                    )
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
                                            dragBarX = -1f
                                        }
                                    }
                                }
                            }
                    ) {
                        // 1. 全体背景 (動画が存在しない背景区間)
                        drawGraphBackground(color = noVideoBgColor)

                        // 2. 動画が存在する範囲
                        drawVideoSegments(
                            videos = uiState.videos,
                            minReal = minReal,
                            spanMs = spanMs,
                            backgroundColor = activeVideoBgColor,
                            borderColor = activeBorderColor,
                            topLineColor = activeTopLineColor
                        )

                        // 3. 集計データバー
                        drawHistogramBars(
                            barCounts = uiState.graphBarCounts,
                            maxBarCount = uiState.maxBarCount,
                            currentRealTimeMs = currentRealTimeMs,
                            minReal = minReal,
                            spanMs = spanMs,
                            scrollOffset = scrollState.value.toFloat(),
                            viewportWidthPx = viewportWidthPx,
                            primaryColor = primaryColor
                        )

                        // 4. 赤バー（インジケーター）
                        drawRedBarIndicator(
                            currentRealTimeMs = currentRealTimeMs,
                            minReal = minReal,
                            spanMs = spanMs,
                            isDragging = isDraggingRedBar,
                            dragBarX = dragBarX,
                            timeScale = timeScale
                        )

                    }

                    // 2. 目盛り描画
                    UnifiedTimeTicks(
                        uiState = uiState,
                        minReal = minReal,
                        spanMs = spanMs,
                        scrollOffset = scrollState.value.toFloat(),
                        viewportWidthPx = viewportWidthPx
                    )
                }
            }
        }
    }
}


/**
 * グラフ上部のヘッダー行 (スケール選択ドロップダウン、前/次の検出オブジェクトへの移動ボタン)
 */
@Composable
private fun UnifiedGraphHeader(
    timeScale: TimeScale,
    hasPrevKeyframe: Boolean,
    hasNextKeyframe: Boolean,
    onTimeScaleSelected: (TimeScale) -> Unit,
    onSeekToPrevKeyframe: () -> Unit,
    onSeekToNextKeyframe: () -> Unit
) {
    // スケールサイズドロップダウンの開閉状態
    var timeScaleMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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

            // ◀ 前のオブジェクトの時点へ移動
            IconButton(
                onClick = onSeekToPrevKeyframe,
                enabled = hasPrevKeyframe,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "前の検出オブジェクトの時点へ移動",
                    tint = if (hasPrevKeyframe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // ▶ 次のオブジェクトの時点へ移動
            IconButton(
                onClick = onSeekToNextKeyframe,
                enabled = hasNextKeyframe,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "次の検出オブジェクトの時点へ移動",
                    tint = if (hasNextKeyframe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = "🔍 ピンチで拡大縮小",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}


/**
 * 画面横スライド・TimeScale に直結・完全連動する目盛り描画コンポーネント
 */
@Composable
fun UnifiedTimeTicks(
    uiState: MovieUiState,
    minReal: Long,
    spanMs: Long,
    scrollOffset: Float,
    viewportWidthPx: Float
) {
    val tickTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val tickLineColor = MaterialTheme.colorScheme.outline
    val subTickLineColor = MaterialTheme.colorScheme.outlineVariant
    val timeScale = uiState.selectedTimeScale

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val canvasWidth = size.width
            val maxReal = minReal + spanMs

            if (canvasWidth <= 0f || spanMs <= 0L) return@Canvas

            val majorIntervalMs = timeScale.majorTickMs
            val minorIntervalMs = timeScale.minorTickMs

            val visibleStartMs = minReal + ((scrollOffset / canvasWidth) * spanMs).toLong()
            val visibleEndMs = minReal + (((scrollOffset + viewportWidthPx) / canvasWidth) * spanMs).toLong()

            // 1. 小目盛り（100%忠実描画、大目盛りと被る位置はスッキリ除外）
            if (minorIntervalMs > 0L) {
                val firstMinor = alignToRoundTime(visibleStartMs, minorIntervalMs)
                val lastMinor = alignToRoundTime(visibleEndMs, minorIntervalMs) + minorIntervalMs
                var t = firstMinor
                while (t <= lastMinor && t <= maxReal + minorIntervalMs) {
                    if (t >= minReal) {
                        val tzOffset = TimeZone.getDefault().getOffset(t)
                        val localMs = t + tzOffset
                        if (localMs % majorIntervalMs != 0L) {
                            val x = ((t - minReal).toDouble() / spanMs.toDouble() * canvasWidth).toFloat()
                            drawLine(
                                color = subTickLineColor.copy(alpha = 0.45f),
                                start = Offset(x, 0f),
                                end = Offset(x, 3.5.dp.toPx()),
                                strokeWidth = 1.0.dp.toPx()
                            )
                        }
                    }
                    t += minorIntervalMs
                }
            }

            // 2. 大目盛り（100%忠実描画）
            val firstMajor = alignToRoundTime(visibleStartMs, majorIntervalMs)
            val lastMajor = alignToRoundTime(visibleEndMs, majorIntervalMs) + majorIntervalMs

            val timeSdf = when (timeScale) {
                TimeScale.THREE_DAYS, TimeScale.ONE_DAY -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                TimeScale.TWELVE_HOURS, TimeScale.SIX_HOURS, TimeScale.THREE_HOURS, TimeScale.ONE_HALF_HOURS, TimeScale.THIRTY_MINUTES, TimeScale.TEN_MINUTES -> SimpleDateFormat("HH:mm", Locale.getDefault())
                TimeScale.FIVE_MINUTES, TimeScale.ONE_MINUTE -> SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            }

            // ラベル描画用の Paint は使い回す (従来はラベルごとに生成していた)
            val labelPaint = android.graphics.Paint().apply {
                color = tickTextColor
                textSize = 10.dp.toPx()
                isAntiAlias = true
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val labelGapPx = 4.dp.toPx()
            // 最後に描いたラベルの右端。次のラベルはこれより右に離れている場合だけ描く (間引き)
            var lastLabelRight = Float.NEGATIVE_INFINITY

            var t = firstMajor
            while (t <= lastMajor && t <= maxReal + majorIntervalMs) {
                if (t >= minReal) {
                    val x = ((t - minReal).toDouble() / spanMs.toDouble() * canvasWidth).toFloat()

                    drawLine(
                        color = tickLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, 7.0.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // 目盛り線はすべて描き、ラベルだけを実測幅ベースで間引く。
                    // 総横幅が上限でクランプされる細かいスケール (1分など) でも
                    // ラベル同士が重ならなくなる。
                    val label = timeSdf.format(Date(t))
                    val labelHalfWidth = labelPaint.measureText(label) / 2f
                    if (x - labelHalfWidth >= lastLabelRight + labelGapPx) {
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            size.height - 2.dp.toPx(),
                            labelPaint
                        )
                        lastLabelRight = x + labelHalfWidth
                    }
                }
                t += majorIntervalMs
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
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
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
                val selectedColor = uiState.selectedColorFilter
                OutlinedTextField(
                    value = selectedColor?.displayName ?: "すべて",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("色絞り込み") },
                    leadingIcon = selectedColor?.let { color ->
                        {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = color.displayName,
                                tint = color.composeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
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
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Pets,
                                        contentDescription = color.displayName,
                                        tint = color.composeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(color.displayName)
                                }
                            },
                            onClick = {
                                onColorFilterSelected(color)
                                colorMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 🐾 足跡（お気に入り）トグルフィルターボタン (隣の OutlinedTextField と高さ・枠線を統一)
            val isLikedSelected = uiState.isLikedFilterOnly
            val borderColor = if (isLikedSelected) Color(0xFFFF9800) else MaterialTheme.colorScheme.outline
            val containerColor = if (isLikedSelected) Color(0xFFFF9800).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface

            Surface(
                onClick = onLikedFilterToggled,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp),
                shape = MaterialTheme.shapes.extraSmall,
                color = containerColor,
                border = BorderStroke(
                    width = if (isLikedSelected) 2.dp else 1.dp,
                    color = borderColor
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Favorite Filter",
                        tint = if (isLikedSelected) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "のみ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isLikedSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLikedSelected) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


/**
 * グラフ全体の背景 (動画が存在しない背景区間: トーンを落としたシックなカラー)
 */
private fun DrawScope.drawGraphBackground(color: Color) {
    drawRoundRect(
        color = color,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
}


/**
 * 動画が存在する範囲の描画 (ハイライト背景、上部インジケーターライン、左右の境界線)
 */
private fun DrawScope.drawVideoSegments(
    videos: List<VideoEntity>,
    minReal: Long,
    spanMs: Long,
    backgroundColor: Color,
    borderColor: Color,
    topLineColor: Color
) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    for (video in videos) {
        val vStart = video.realStartTime
        val vEnd = video.realEndTime
        if (vEnd >= minReal && vStart <= minReal + spanMs) {
            val startProgress = ((vStart - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
            val endProgress = ((vEnd - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
            val left = startProgress * canvasWidth
            val right = endProgress * canvasWidth
            val width = (right - left).coerceAtLeast(3f)

            // 鮮やかなハイライト背景で動画存在領域を強調
            drawRect(
                color = backgroundColor,
                topLeft = Offset(left, 0f),
                size = Size(width, canvasHeight)
            )

            // 動画領域の上部インジケーターライン
            drawRect(
                color = topLineColor,
                topLeft = Offset(left, 0f),
                size = Size(width, 4.dp.toPx())
            )

            // 左右のクッキリした境界線
            drawLine(
                color = borderColor,
                start = Offset(left, 0f),
                end = Offset(left, canvasHeight),
                strokeWidth = 2.5.dp.toPx()
            )
            drawLine(
                color = borderColor,
                start = Offset(right, 0f),
                end = Offset(right, canvasHeight),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}


/**
 * 集計データバー (ヒストグラム) の描画 (可視範囲分のみ)
 */
private fun DrawScope.drawHistogramBars(
    barCounts: List<Int>,
    maxBarCount: Int,
    currentRealTimeMs: Long,
    minReal: Long,
    spanMs: Long,
    scrollOffset: Float,
    viewportWidthPx: Float,
    primaryColor: Color
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val totalBarCount = barCounts.size
    if (totalBarCount <= 0) return

    val barWidth = canvasWidth / totalBarCount.toFloat()

    val firstVisibleIndex = floor(scrollOffset / barWidth).toInt().coerceAtLeast(0)
    val lastVisibleIndex = ceil((scrollOffset + viewportWidthPx) / barWidth).toInt().coerceAtMost(totalBarCount - 1)

    val maxCount = maxBarCount.toFloat().coerceAtLeast(1f)
    val cProgress = if (spanMs > 0L) {
        ((currentRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val currentBarIndex = (cProgress * totalBarCount).toInt().coerceIn(0, totalBarCount - 1)

    for (i in firstVisibleIndex..lastVisibleIndex) {
        val count = barCounts.getOrNull(i) ?: 0
        if (count <= 0) continue

        val left = i * barWidth
        val drawWidth = (barWidth * 0.82f).coerceAtLeast(1f)
        val inset = (barWidth - drawWidth) / 2f

        val barHeightRatio = count.toFloat() / maxCount
        val barHeightPx = (canvasHeight * 0.75f) * barHeightRatio
        val top = canvasHeight - barHeightPx

        val isCurrent = (i == currentBarIndex)
        val color = if (isCurrent) Color.Red else primaryColor

        drawRoundRect(
            color = color,
            topLeft = Offset(left + inset, top),
            size = Size(drawWidth, barHeightPx),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }
}


/**
 * 赤バー (現在位置インジケーター) と時刻ラベルの描画
 */
private fun DrawScope.drawRedBarIndicator(
    currentRealTimeMs: Long,
    minReal: Long,
    spanMs: Long,
    isDragging: Boolean,
    dragBarX: Float,
    timeScale: TimeScale
) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    val cProgress = if (spanMs > 0L) {
        ((currentRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val activeIndicatorX = if (isDragging && dragBarX >= 0f) {
        dragBarX
    } else {
        cProgress * canvasWidth
    }

    val lineStrokeWidth = if (isDragging) 4.dp.toPx() else 2.5.dp.toPx()

    drawLine(
        color = Color.Red,
        start = Offset(activeIndicatorX, 0f),
        end = Offset(activeIndicatorX, canvasHeight),
        strokeWidth = lineStrokeWidth
    )

    val displayMs = if (isDragging && dragBarX >= 0f) {
        val ratio = (dragBarX / canvasWidth).coerceIn(0f, 1f)
        minReal + (ratio * spanMs).toLong()
    } else {
        currentRealTimeMs
    }

    val dateTimeSdf = when (timeScale) {
        TimeScale.THREE_DAYS, TimeScale.ONE_DAY -> SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
        else -> SimpleDateFormat("HH:mm:ss", Locale.getDefault())
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
            color = if (isDragging) Color(0xFFFF3333) else Color.Red,
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


/**
 * Epochミリ秒をタイムゾーン考慮の「キリの良い正時（00分, 15分, 30分や00秒等）」に切り揃えるヘルパー関数
 */
private fun alignToRoundTime(timeMs: Long, intervalMs: Long): Long {
    if (intervalMs <= 0L) return timeMs
    val tzOffset = TimeZone.getDefault().getOffset(timeMs)
    val localMs = timeMs + tzOffset
    val alignedLocalMs = (localMs / intervalMs) * intervalMs
    return alignedLocalMs - tzOffset
}


/**
 * 指の位置 (rawX) から、動画が存在する区間の中だけに制限した赤バーの表示位置を算出する。
 * 動画のない時間帯では、バーは直前の動画区間の端 (末尾/先頭) で止まり、
 * 指が次の動画区間に到達するとそこから追随を再開する。
 * バーが動画区間に無い状態で開始された場合は、最も近い動画の端に置く。
 *
 * rawX はまず描画幅の範囲に丸める。右端の自動スクロールでは指位置が要素全体幅まで
 * 進み得るため、丸めずに時刻換算すると割合が 1.0 に張り付き、軸の右端=最後の動画の
 * 終端の場合に「区間内」と誤判定してバーが終端を超えてしまう。
 */
private fun snapDragBarXToVideoRange(
    videos: List<VideoEntity>,
    minReal: Long,
    spanMs: Long,
    canvasWidth: Float,
    rawX: Float,
    lastBarX: Float
): Float {
    if (canvasWidth <= 0f || spanMs <= 0L) return rawX
    val clampedRawX = rawX.coerceIn(0f, canvasWidth)

    val sortedVideos = videos.sortedBy { it.realStartTime }
    if (sortedVideos.isEmpty()) return clampedRawX

    fun timeToX(timeMs: Long): Float =
        ((timeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f) * canvasWidth

    fun xToTimeMs(x: Float): Long =
        minReal + ((x / canvasWidth).coerceIn(0f, 1f) * spanMs).toLong()

    val rawTimeMs = xToTimeMs(clampedRawX)
    if (sortedVideos.any { rawTimeMs in it.realStartTime..it.realEndTime }) {
        return clampedRawX
    }

    // 直前のバー位置を含む動画区間の端で止める
    if (lastBarX >= 0f) {
        val lastTimeMs = xToTimeMs(lastBarX)
        val lastVideo = sortedVideos.firstOrNull {
            lastTimeMs in it.realStartTime..it.realEndTime
        }
        if (lastVideo != null) {
            return if (rawTimeMs > lastTimeMs) {
                timeToX(lastVideo.realEndTime)
            } else {
                timeToX(lastVideo.realStartTime)
            }
        }
    }

    // バーが動画区間に無い状態でドラッグを開始した場合: 最も近い動画の端へ置く
    var nearestEdgeMs = sortedVideos.first().realStartTime
    var nearestDist = Long.MAX_VALUE
    for (video in sortedVideos) {
        for (edgeMs in listOf(video.realStartTime, video.realEndTime)) {
            val dist = kotlin.math.abs(edgeMs - rawTimeMs)
            if (dist < nearestDist) {
                nearestDist = dist
                nearestEdgeMs = edgeMs
            }
        }
    }
    return timeToX(nearestEdgeMs)
}
