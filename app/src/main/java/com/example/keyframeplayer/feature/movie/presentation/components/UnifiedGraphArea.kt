package com.example.keyframeplayer.feature.movie.presentation.components

import android.util.Log
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
import java.util.TimeZone
import kotlin.math.ceil
import kotlin.math.floor

/**
 * TimeScale に連動した主目盛り（大きな目盛り）間隔 (ミリ秒) を取得
 */
fun TimeScale.getTickIntervalMs(): Long = majorTickMs

/**
 * TimeScale に連動した副目盛り（小さい目盛り線）間隔 (ミリ秒) を取得
 */
fun TimeScale.getSubTickIntervalMs(): Long = minorTickMs

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
    var localDragX by remember { mutableFloatStateOf(-1f) }

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
                        onClick = {
                            prevKeyframe?.let { kf ->
                                if (spanMs > 0L) {
                                    val progress = ((kf.realTime - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                    onProgressTapped(progress)
                                }
                            }
                        },
                        enabled = prevKeyframe != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "前の検出オブジェクトの時点へ移動",
                            tint = if (prevKeyframe != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // ▶ 次のオブジェクトの時点へ移動
                    IconButton(
                        onClick = {
                            nextKeyframe?.let { kf ->
                                if (spanMs > 0L) {
                                    val progress = ((kf.realTime - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                    onProgressTapped(progress)
                                }
                            }
                        },
                        enabled = nextKeyframe != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "次の検出オブジェクトの時点へ移動",
                            tint = if (nextKeyframe != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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

                        // 1. 全体背景 (動画が存在しない背景区間: トーンを落としたシックなカラー)
                        drawRoundRect(
                            color = noVideoBgColor,
                            size = Size(canvasWidth, canvasHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // 2. 動画が存在する範囲 (ここを圧倒的にクッキリと強調表示！)
                        for (video in uiState.videos) {
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
                                    color = activeVideoBgColor,
                                    topLeft = Offset(left, 0f),
                                    size = Size(width, canvasHeight)
                                )

                                // 動画領域の上部インジケーターライン
                                drawRect(
                                    color = activeTopLineColor,
                                    topLeft = Offset(left, 0f),
                                    size = Size(width, 4.dp.toPx())
                                )

                                // 左右のクッキリした境界線
                                drawLine(
                                    color = activeBorderColor,
                                    start = Offset(left, 0f),
                                    end = Offset(left, canvasHeight),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                                drawLine(
                                    color = activeBorderColor,
                                    start = Offset(right, 0f),
                                    end = Offset(right, canvasHeight),
                                    strokeWidth = 2.5.dp.toPx()
                                )
                            }
                        }

                        // 3. 集計データバーの描画 (画面1幅にぴったり90分割分が入る完璧なアスペクト比で描画)
                        val barCounts = uiState.graphBarCounts
                        val totalBarCount = barCounts.size
                        if (totalBarCount > 0) {
                            val scrollOffset = scrollState.value.toFloat()
                            val barWidth = canvasWidth / totalBarCount.toFloat()

                            val firstVisibleIndex = floor(scrollOffset / barWidth).toInt().coerceAtLeast(0)
                            val lastVisibleIndex = ceil((scrollOffset + viewportWidthPx) / barWidth).toInt().coerceAtMost(totalBarCount - 1)

                            val maxCount = uiState.maxBarCount.toFloat().coerceAtLeast(1f)
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

                    // 2. 選択された TimeScale で指定された majorTickMs / minorTickMs を詳細ログ出力しつつ描画
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
 * 画面横スライド・TimeScale に直結・完全連動し、目盛り計算の詳細パラメータを Logcat に出力するコンポーネント
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

            val debugSdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault())

            Log.d("UnifiedTimeTicks", "==================================================")
            Log.d("UnifiedTimeTicks", "[Scale] label=${timeScale.label}, majorTickMs=$majorIntervalMs (${majorIntervalMs / 1000}s), minorTickMs=$minorIntervalMs (${minorIntervalMs / 1000}s)")
            Log.d("UnifiedTimeTicks", "[Canvas] width=${canvasWidth}px, viewportWidth=${viewportWidthPx}px, scrollOffset=${scrollOffset}px")
            Log.d("UnifiedTimeTicks", "[Timeline Bounds] minReal=${debugSdf.format(Date(minReal))}, maxReal=${debugSdf.format(Date(maxReal))}, spanMs=${spanMs}ms (${spanMs / 1000}s)")
            Log.d("UnifiedTimeTicks", "[Visible Range] Left(Start)=${debugSdf.format(Date(visibleStartMs))}, Right(End)=${debugSdf.format(Date(visibleEndMs))}, 1画面表示幅=${(visibleEndMs - visibleStartMs)}ms (${(visibleEndMs - visibleStartMs) / 1000}s)")

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

            Log.d("UnifiedTimeTicks", "[Major Loop] firstMajor=${debugSdf.format(Date(firstMajor))}, lastMajor=${debugSdf.format(Date(lastMajor))}")

            val timeSdf = when (timeScale) {
                TimeScale.THREE_DAYS, TimeScale.ONE_DAY -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                TimeScale.TWELVE_HOURS, TimeScale.SIX_HOURS, TimeScale.THREE_HOURS, TimeScale.ONE_HALF_HOURS, TimeScale.THIRTY_MINUTES, TimeScale.TEN_MINUTES -> SimpleDateFormat("HH:mm", Locale.getDefault())
                TimeScale.FIVE_MINUTES, TimeScale.ONE_MINUTE -> SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            }

            var t = firstMajor
            var count = 0
            while (t <= lastMajor && t <= maxReal + majorIntervalMs) {
                if (t >= minReal) {
                    val x = ((t - minReal).toDouble() / spanMs.toDouble() * canvasWidth).toFloat()

                    drawLine(
                        color = tickLineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, 7.0.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    val label = timeSdf.format(Date(t))
                    count++

                    Log.d("UnifiedTimeTicks", "  -> 大目盛り #$count: 時刻=${debugSdf.format(Date(t))} (表示ラベル='$label'), 描画x座標=${x}px")

                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        x,
                        size.height - 2.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = tickTextColor
                            textSize = 10.dp.toPx()
                            isAntiAlias = true
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
                t += majorIntervalMs
            }
            Log.d("UnifiedTimeTicks", "==================================================")
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

            // 🐾 足跡（お気に入り）トグルフィルターボタン
            FilterChip(
                selected = uiState.isLikedFilterOnly,
                onClick = onLikedFilterToggled,
                label = {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Favorite Filter",
                        tint = if (uiState.isLikedFilterOnly) Color(0xFFFF9800) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}
