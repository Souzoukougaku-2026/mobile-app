package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.toArgb
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

/**
 * =========================================================================================
 * 【赤バー（シークインジケーター）移動およびジェスチャー描画における実装仕様・制約ドキュメント】
 * =========================================================================================
 * 
 * 1. タイムスケールとページング (HorizontalPager) の構造的制約:
 *    - グラフ全体は 1ページあたり [scaleWindowMs]（選択された TimeScale の時間幅）単位で
 *      HorizontalPager により仮想化ページング描画されています。
 *    - 各ページは独自の Canvas 座標空間 (0f .. canvasWidth) を持っており、赤バーの現在位置は
 *      [pageStartMs .. pageEndMs] に対する相対割合 (0.0 .. 1.0) で算出されます。
 *
 * 2. ドラッグ中の追従・アニメーション遅延 (animateFloatAsState) の回避:
 *    - 指で赤バーをドラッグ操作している間、animateFloatAsState や過度なスプリング補間を適用すると、
 *      指の移動に対して赤バーが遅れて追従したり、指を放した瞬間にアニメーションで位置が跳ね戻る等、
 *      ユーザー体験（操作感）の違和感・ブレの原因となります。
 *    - したがって、ドラッグ中 (isDraggingRedBar) は補間なしで現在位置を直接算出して描画し、
 *      指の動きにダイレクトに同期させます。
 *
 * 3. ページ境界（エッジ）跨ぎドラッグ時のポインターイベント空間の断絶:
 *    - ユーザーが赤バーを掴んだまま画面端に到達すると、ページの自動切り替え (scrollToPage) が発生します。
 *    - しかし pointerInput は各ページの Canvas 内にバインドされているため、ページが切り替わった瞬間に
 *      旧ページのポインター座標と新ページの座標系との間に不連続（ワープ現象）が発生します。
 *    - 対策: 赤バー操作中 (isDraggingRedBar) は HorizontalPager の手動ページスライドを無効化
 *      (userScrollEnabled = !isDraggingRedBar) し、エッジホールド時にタイマーで連続時間加算を行います。
 *
 * 4. ExoPlayer シーク (seekTo) 高頻度呼び出しによるフィードバックループ:
 *    - 赤バーのドラッグ中に毎フレーム (16msごと) Player へ seekTo を高頻度で送ると、動画デコーダーの
 *      バッファリング遅延が発生し、ExoPlayer の再生位置同期とドラッグ位置の間でフィードバックループが起きます。
 *    - 対策: MovieViewModel 側で lastSeekTimestamp による 800ms の位置同期インターロックを設けています。
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

    // ページング数計算（1ページ = 選択時間スケールの表示幅）
    val pageCount = maxOf(1, ceil(spanMs.toDouble() / scaleWindowMs.toDouble()).toInt())

    // 現在再生中の時刻が属するページインデックス
    val currentRealTimeMs = uiState.currentRealTimeMs
    val targetPage = if (scaleWindowMs > 0L) {
        ((currentRealTimeMs - minReal) / scaleWindowMs).toInt().coerceIn(0, pageCount - 1)
    } else 0

    val pagerState = rememberPagerState(
        initialPage = targetPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    // 再生位置が変わった際に pager を自動スクロール追従（手動操作中を除く）
    LaunchedEffect(targetPage, pageCount) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage.coerceIn(0, pageCount - 1))
        }
    }

    // ピンチジェスチャーの累積倍率トラッキング
    var accumulatedZoom by remember { mutableFloatStateOf(1.0f) }

    // スケールサイズドロップダウンの開閉状態
    var timeScaleMenuExpanded by remember { mutableStateOf(false) }

    Box(
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // ページインジケーター & スケールサイズドロップダウン & ズーム操作ヒント
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
                    Text(
                        text = "ページ ${pagerState.currentPage + 1} / $pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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

            // 赤バーのドラッグ状態（ドラッグ中は Pager の手動スライドをロック）
            var isDraggingRedBar by remember { mutableStateOf(false) }
            var localDragX by remember { mutableFloatStateOf(-1f) }

            // 1. HorizontalPager によるシームレスな巻物風ページング処理
            HorizontalPager(
                state = pagerState,
                pageSpacing = 0.dp,
                beyondViewportPageCount = 1,
                userScrollEnabled = !isDraggingRedBar,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val pageStartMs = minReal + page * scaleWindowMs
                val pageEndMs = pageStartMs + scaleWindowMs

                Column(modifier = Modifier.fillMaxWidth()) {
                    val primaryColor = MaterialTheme.colorScheme.primary

                    val currentRealMs = uiState.currentRealTimeMs
                    val isCurrentInPage = currentRealMs in pageStartMs..pageEndMs
                    val currentProgressInPage = if (isCurrentInPage) {
                        ((currentRealMs - pageStartMs).toFloat() / scaleWindowMs.toFloat()).coerceIn(0f, 1f)
                    } else -1f

                    val noVideoBgColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    val activeVideoBgColor = MaterialTheme.colorScheme.surface
                    val activeBorderColor = primaryColor.copy(alpha = 0.6f)

                    // ===== [DEBUG MARKER: 指の位置描画用 (コメントアウト/削除で簡単に無効化可能)] =====
                    var debugTouchPosition by remember { mutableStateOf<Offset?>(null) }
                    // =========================================================================

                    // 1ページあたりの Canvas 描画
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                            .padding(horizontal = 16.dp)
                            .pointerInput(page, pageStartMs, scaleWindowMs, spanMs, minReal) {
                                val edgeThresholdPx = 48.dp.toPx()

                                awaitPointerEventScope {
                                    while (true) {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val canvasWidth = size.width.toFloat()
                                        if (canvasWidth <= 0f) continue

                                        // ===== [DEBUG MARKER: タッチ位置記憶] =====
                                        debugTouchPosition = down.position

                                        fun updateProgressForX(x: Float) {
                                            val pageProgress = (x / canvasWidth).coerceIn(0f, 1f)
                                            val clickedRealTimeMs = pageStartMs + (pageProgress * scaleWindowMs).toLong()
                                            val overallProgress = ((clickedRealTimeMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                            currentOnProgressTapped(overallProgress)
                                        }

                                        // 指を下ろした位置に即座に赤バーを移動して直感的にドラッグ開始
                                        down.consume()
                                        isDraggingRedBar = true
                                        localDragX = down.position.x

                                        var currentX = down.position.x
                                        val pointerId = down.id

                                        // 画面端に指がある場合の自動スクロール (端を維持する間 200ms 周期で時間軸をゆったり自動移動)
                                        val autoScrollJob = coroutineScope.launch {
                                            while (isActive) {
                                                delay(200L) // ゆったりした周期で無理な連続シーク負荷を防止

                                                val state = currentUiState
                                                if (currentX > canvasWidth - edgeThresholdPx) {
                                                    val ratio = ((currentX - (canvasWidth - edgeThresholdPx)) / edgeThresholdPx).coerceIn(0.1f, 1f)
                                                    val stepMs = (scaleWindowMs * 0.05f * ratio).toLong().coerceAtLeast(100L)
                                                    val newMs = (state.currentRealTimeMs + stepMs).coerceAtMost(minReal + spanMs)
                                                    val overallProgress = ((newMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                                    currentOnProgressTapped(overallProgress)
                                                } else if (currentX < edgeThresholdPx) {
                                                    val ratio = ((edgeThresholdPx - currentX) / edgeThresholdPx).coerceIn(0.1f, 1f)
                                                    val stepMs = (scaleWindowMs * 0.05f * ratio).toLong().coerceAtLeast(100L)
                                                    val newMs = (state.currentRealTimeMs - stepMs).coerceAtLeast(minReal)
                                                    val overallProgress = ((newMs - minReal).toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
                                                    currentOnProgressTapped(overallProgress)
                                                }
                                            }
                                        }

                                        try {
                                            // ドラッグ中は表示位置(localDragX)のみを追従させ、動画シーク連打による重い処理を回避
                                            drag(pointerId) { change ->
                                                change.consume()
                                                currentX = change.position.x
                                                localDragX = currentX
                                                // ===== [DEBUG MARKER: ドラッグ移動位置の更新] =====
                                                debugTouchPosition = change.position
                                            }
                                        } finally {
                                            autoScrollJob.cancel()
                                            // 指を離した瞬間に最終確定位置へ 1回だけシーク実行
                                            updateProgressForX(currentX)
                                            isDraggingRedBar = false
                                            localDragX = -1f
                                            // ===== [DEBUG MARKER: ドラッグ解除] =====
                                            debugTouchPosition = null
                                        }
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        // 1. 全体背景（動画が存在しない背景区間: 暗めのトーン）
                        drawRoundRect(
                            color = noVideoBgColor,
                            size = Size(canvasWidth, canvasHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // 2. 動画が存在する範囲（明るいハイライト領域 & 境界線）
                        for (video in uiState.videos) {
                            val vStart = video.realStartTime
                            val vEnd = video.realEndTime
                            if (vEnd >= pageStartMs && vStart <= pageEndMs) {
                                val startProgress = ((vStart - pageStartMs).toFloat() / scaleWindowMs.toFloat()).coerceIn(0f, 1f)
                                val endProgress = ((vEnd - pageStartMs).toFloat() / scaleWindowMs.toFloat()).coerceIn(0f, 1f)
                                val left = startProgress * canvasWidth
                                val right = endProgress * canvasWidth
                                val width = (right - left).coerceAtLeast(2f)

                                // 動画が存在するエリアを「明」色で表示
                                drawRect(
                                    color = activeVideoBgColor,
                                    topLeft = Offset(left, 0f),
                                    size = Size(width, canvasHeight)
                                )

                                // 動画範囲の境界線を描画
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

                        // 3. 集計データバーの描画 (画面横幅に1/90幅のバーを90本均等配置)
                        val barCountInWindow = timeScale.barCountPerWindow
                        val spacing = 0.5.dp.toPx()
                        val barWidth = ((canvasWidth - (spacing * (barCountInWindow + 1))) / barCountInWindow).coerceAtLeast(1f)
                        val stepWidth = barWidth + spacing

                        val startBarIndex = page * barCountInWindow
                        val barCounts = uiState.graphBarCounts
                        val maxCount = uiState.maxBarCount.toFloat().coerceAtLeast(1f)

                        val currentBarIndexInPage = if (currentProgressInPage >= 0f) {
                            (currentProgressInPage * barCountInWindow).toInt().coerceIn(0, barCountInWindow - 1)
                        } else -1

                        for (i in 0 until barCountInWindow) {
                            val globalBarIndex = startBarIndex + i
                            val count = barCounts.getOrNull(globalBarIndex) ?: 0
                            if (count <= 0) continue

                            val left = spacing + (i * stepWidth)
                            val barHeightRatio = count.toFloat() / maxCount
                            val barHeightPx = (canvasHeight * 0.75f) * barHeightRatio
                            val top = canvasHeight - barHeightPx

                            val isCurrent = (i == currentBarIndexInPage)
                            val color = if (isCurrent) Color.Red else primaryColor

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeightPx),
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                            )
                        }

                        // 4. 現在位置インジケーター (滑らかに動く赤線 & 日時バッジ)
                        val activeIndicatorX = if (isDraggingRedBar && localDragX >= 0f) {
                            localDragX
                        } else if (isCurrentInPage && currentProgressInPage >= 0f) {
                            currentProgressInPage * canvasWidth
                        } else -1000f

                        val showIndicator = isDraggingRedBar || (isCurrentInPage && currentProgressInPage >= 0f)

                        if (showIndicator && activeIndicatorX >= 0f) {
                            val lineStrokeWidth = if (isDraggingRedBar) 4.dp.toPx() else 2.5.dp.toPx()

                            drawLine(
                                color = Color.Red,
                                start = Offset(activeIndicatorX, 0f),
                                end = Offset(activeIndicatorX, canvasHeight),
                                strokeWidth = lineStrokeWidth
                            )

                            val displayMs = if (isDraggingRedBar && localDragX >= 0f) {
                                val pageRatio = (localDragX / canvasWidth).coerceIn(0f, 1f)
                                pageStartMs + (pageRatio * scaleWindowMs).toLong()
                            } else {
                                currentRealMs
                            }

                            val dateTimeSdf = SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault())
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

                        // ===== [DEBUG MARKER: 指の位置表示用 (不要になったらコメントアウトまたは削除)] =====
                        debugTouchPosition?.let { touchPos ->
                            // 指のX座標を示す緑の垂直ライン
                            drawLine(
                                color = Color.Green,
                                start = Offset(touchPos.x, 0f),
                                end = Offset(touchPos.x, canvasHeight),
                                strokeWidth = 2.dp.toPx()
                            )

                            // 指のタッチ位置を中心とするシアン色の大きなマーカー円
                            drawCircle(
                                color = Color.Cyan.copy(alpha = 0.65f),
                                radius = 22.dp.toPx(),
                                center = touchPos
                            )
                        }
                        // =========================================================================
                    }

                    // ページ単位のタイム目盛り
                    PageTimeTicks(
                        pageStartMs = pageStartMs,
                        scaleWindowMs = scaleWindowMs
                    )
                }
            }
        }
    }
}

/**
 * ページ内動的「日時」目盛りラベル
 */
@Composable
fun PageTimeTicks(
    pageStartMs: Long,
    scaleWindowMs: Long
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
            val pageEndMs = pageStartMs + scaleWindowMs

            if (canvasWidth <= 0f || scaleWindowMs <= 0L) return@Canvas

            val minSpacingPx = 70.dp.toPx()
            val msPerPx = scaleWindowMs.toDouble() / canvasWidth.toDouble()
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

            val firstTick = (pageStartMs / intervalMs) * intervalMs
            val lastTick = ((pageEndMs / intervalMs) + 1) * intervalMs

            val secTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val minTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateTimeFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            val isMultiDays = scaleWindowMs > 24 * 60 * 60 * 1000L

            var tickTimeMs = firstTick
            var lastDrawnX = -1000f

            while (tickTimeMs <= lastTick) {
                if (tickTimeMs in pageStartMs..pageEndMs) {
                    val progress = ((tickTimeMs - pageStartMs).toDouble() / scaleWindowMs.toDouble()).coerceIn(0.0, 1.0)
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
                            isMultiDays -> dateTimeFormat.format(Date(tickTimeMs))
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
