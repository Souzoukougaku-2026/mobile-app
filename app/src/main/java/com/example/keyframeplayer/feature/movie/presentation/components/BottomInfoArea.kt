package com.example.keyframeplayer.feature.movie.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.core.domain.model.japaneseClassName
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

/**
 * 動画詳細画面の下部エリア: 検出画像とその情報を表示
 */
@Composable
fun BottomInfoArea(
    currentTime: Float,
    activeKeyframe: CropImage? = null,
    keyframes: List<CropImage> = emptyList(),
    selectedClassFilter: String? = null,
    selectedColorFilter: ImageColor? = null,
    isLikedFilterOnly: Boolean = false,
    realTimeMs: Long = 0L,
    onSwipeLeft: (CropImage?) -> Unit = {},
    onSwipeRight: (CropImage?) -> Unit = {},
    onKeyframeLikedStateChanged: (UUID, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // 1. クラス & 色 & いいね フィルター適用
    val filteredKeyframes = remember(keyframes, selectedClassFilter, selectedColorFilter, isLikedFilterOnly) {
        keyframes.filter { item ->
            (selectedClassFilter == null || item.className == selectedClassFilter) &&
            (selectedColorFilter == null || item.color == selectedColorFilter) &&
            (!isLikedFilterOnly || item.isLiked)
        }
    }

    // 2. 現在の動画再生位置・アクティブキーフレームに基づく対象キーフレームの抽出
    val currentKeyframe = remember(filteredKeyframes, activeKeyframe, realTimeMs) {
        if (activeKeyframe != null && filteredKeyframes.contains(activeKeyframe)) {
            activeKeyframe
        } else if (realTimeMs > 0L && filteredKeyframes.isNotEmpty()) {
            filteredKeyframes.minByOrNull { abs(it.realTime - realTimeMs) }
        } else {
            filteredKeyframes.firstOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentKeyframe != null) {
            val currentIndex = filteredKeyframes.indexOf(currentKeyframe)

            // ヘッダー (タイトル & お気に入り切り替えボタン)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
//                Text(
//                    text = "📸 検出オブジェクト詳細 (${currentIndex + 1} / ${filteredKeyframes.size}件)",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.primary
//                )

                IconButton(
                    onClick = {
                        onKeyframeLikedStateChanged(currentKeyframe.id, !currentKeyframe.isLiked)
                    }
                ) {
                    Icon(
                        imageVector = if (currentKeyframe.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (currentKeyframe.isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 画像と詳細情報のメインカード
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 画像表示エリア
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(12.dp))
                    ) {
                        ZoomableImage(
                            imagePath = currentKeyframe.keyFramePath,
                            isPixelCoordinates = true,
                            initialTopLeft = Offset(currentKeyframe.bboxLeft.toFloat(), currentKeyframe.bboxTop.toFloat()),
                            initialBottomRight = Offset(currentKeyframe.bboxRight.toFloat(), currentKeyframe.bboxBottom.toFloat()),
                            modifier = Modifier.fillMaxSize()
                        )

                        // 左上: クラス名・スコアのバッジ
                        Surface(
                            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentKeyframe.japaneseClassName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Score: %.2f".format(currentKeyframe.score),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 詳細情報テーブル
                    val timeStr = if (currentKeyframe.realTime > 0L) {
                        SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault()).format(Date(currentKeyframe.realTime))
                    } else if (realTimeMs > 0L) {
                        SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault()).format(Date(realTimeMs))
                    } else {
                        "%02d:%02d".format((currentTime / 3600).toInt(), ((currentTime % 3600) / 60).toInt())
                    }

                    val tableData = listOf(
                        "Timestamp" to timeStr,
                        "Class Name" to currentKeyframe.japaneseClassName,
//                        "Confidence Score" to "%.2f".format(currentKeyframe.score),
                        "Color Tag" to currentKeyframe.color.displayName,
//                        "BBox Coordinates" to "[${currentKeyframe.bboxLeft}, ${currentKeyframe.bboxTop}, ${currentKeyframe.bboxRight}, ${currentKeyframe.bboxBottom}]",
//                        "Favorite State" to if (currentKeyframe.isLiked) "❤️ Liked" else "🤍 Normal"
                    )

                    VerticalTable(data = tableData)
                }
            }
        } else {
            // 画像がない場合の表示
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🔍 検出画像はありません",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "現在のフィルター条件に合致する検出オブジェクトはありません。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * (旧TinderUI版) コード保持用コンポーザブル
 */
@Composable
fun TinderBottomInfoArea(
    currentTime: Float,
    activeKeyframe: CropImage? = null,
    keyframes: List<CropImage> = emptyList(),
    selectedClassFilter: String? = null,
    selectedColorFilter: ImageColor? = null,
    isLikedFilterOnly: Boolean = false,
    realTimeMs: Long = 0L,
    onSwipeLeft: (CropImage?) -> Unit = {},
    onSwipeRight: (CropImage?) -> Unit = {},
    onKeyframeLikedStateChanged: (UUID, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // 1. クラス & 色 & いいね フィルター適用
    val filteredKeyframes = remember(keyframes, selectedClassFilter, selectedColorFilter, isLikedFilterOnly) {
        keyframes.filter { item ->
            (selectedClassFilter == null || item.className == selectedClassFilter) &&
            (selectedColorFilter == null || item.color == selectedColorFilter) &&
            (!isLikedFilterOnly || item.isLiked)
        }
    }

    // 2. 左スワイプ (NOPE) で対象外（除外）にした画像 ID のセット
    var excludedIds by remember { mutableStateOf(setOf<UUID>()) }

    // フィルター条件変更時は除外状態をリセット
    LaunchedEffect(selectedClassFilter, selectedColorFilter, isLikedFilterOnly) {
        excludedIds = emptySet()
    }

    // 3. 現在の動画再生時刻 (realTimeMs) に近いものを優先的にソート & 未除外のみ抽出
    val visibleCardList = remember(filteredKeyframes, realTimeMs, excludedIds) {
        val available = filteredKeyframes.filter { it.id !in excludedIds }
        if (realTimeMs > 0L) {
            available.sortedBy { abs(it.realTime - realTimeMs) }
        } else {
            available
        }
    }

    var isFlipped by remember { mutableStateOf(false) }

    val currentCard = visibleCardList.firstOrNull()
    val nextCard = visibleCardList.getOrNull(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥 忘れ物？ カード (残 ${visibleCardList.size}件)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (visibleCardList.isNotEmpty()) {
                Text(
                    text = if (isFlipped) "📄 裏面 (詳細データ)" else "🖼️ 表面 (画像)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // カードスタック領域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            if (visibleCardList.isEmpty()) {
                // 画像がなくなった場合の表示
                EmptyTinderCard(
                    onReset = { excludedIds = emptySet() }
                )
            } else {
                // 背面（2枚目）のカード
                if (nextCard != null) {
                    KeyframeFlippableCard(
                        cropImage = nextCard,
                        currentTime = currentTime,
                        realTimeMs = realTimeMs,
                        isFlipped = false,
                        onToggleFlip = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 0.92f
                                scaleY = 0.92f
                                translationY = 24.dp.toPx()
                                alpha = 0.7f
                            }
                    )
                }

                // 最前面（1枚目）のカード
                if (currentCard != null) {
                    SwipeableFlippableTinderCard(
                        cropImage = currentCard,
                        currentTime = currentTime,
                        realTimeMs = realTimeMs,
                        isFlipped = isFlipped,
                        onToggleFlip = { isFlipped = !isFlipped },
                        onSwipedLeft = {
                            // 左スワイプ: 対象外にして除外リストに追加
                            excludedIds = excludedIds + currentCard.id
                            onSwipeLeft(currentCard)
                            isFlipped = false
                        },
                        onSwipedRight = {
                            // 右スワイプ: いいね(DB保存) & シーク連動
                            excludedIds = excludedIds + currentCard.id
                            onKeyframeLikedStateChanged(currentCard.id, true)
                            onSwipeRight(currentCard)
                            isFlipped = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tinder アクションボタン群 (❌ DISLIKE / 🔄 FLIP または RESET / ❤️ LIKE)
        TinderActionButtons(
            hasCards = visibleCardList.isNotEmpty(),
            onDislike = {
                if (currentCard != null) {
                    excludedIds = excludedIds + currentCard.id
                    onSwipeLeft(currentCard)
                    isFlipped = false
                }
            },
            onFlip = {
                isFlipped = !isFlipped
            },
            onReset = {
                excludedIds = emptySet()
            },
            onLike = {
                if (currentCard != null) {
                    excludedIds = excludedIds + currentCard.id
                    onKeyframeLikedStateChanged(currentCard.id, true)
                    onSwipeRight(currentCard)
                    isFlipped = false
                }
            }
        )
    }
}

/**
 * 左右スワイプ可能かつタップで表裏反転（フリップ）可能な Tinder カード
 */
@Composable
fun SwipeableFlippableTinderCard(
    cropImage: CropImage,
    currentTime: Float,
    realTimeMs: Long,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val density = LocalDensity.current
    val swipeThreshold = with(density) { 120.dp.toPx() }

    val rotation = (offsetX.value / 15f).coerceIn(-25f, 25f)
    val swipeProgress = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation
            }
            .pointerInput(cropImage) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetX.value > swipeThreshold) {
                                offsetX.animateTo(1000f, spring())
                                onSwipedRight()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            } else if (offsetX.value < -swipeThreshold) {
                                offsetX.animateTo(-1000f, spring())
                                onSwipedLeft()
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            } else {
                                launch { offsetX.animateTo(0f, spring()) }
                                launch { offsetY.animateTo(0f, spring()) }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        KeyframeFlippableCard(
            cropImage = cropImage,
            currentTime = currentTime,
            realTimeMs = realTimeMs,
            isFlipped = isFlipped,
            onToggleFlip = onToggleFlip
        )

        // ドラッグ中に表示される LIKE / NOPE オーバーレイ
        if (offsetX.value > 20f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .rotate(-15f)
                    .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f * swipeProgress))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LIKE",
                    color = Color(0xFF4CAF50),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (offsetX.value < -20f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .rotate(15f)
                    .border(3.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935).copy(alpha = 0.15f * swipeProgress))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "NOPE",
                    color = Color(0xFFE53935),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 3D フリップ (表:画像 / 裏:データ) カード本体
 */
@Composable
fun KeyframeFlippableCard(
    cropImage: CropImage,
    currentTime: Float,
    realTimeMs: Long,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "CardFlipAnimation"
    )

    val density = LocalDensity.current

    ElevatedCard(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density.density
            }
            .clickable { onToggleFlip() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        if (rotationY <= 90f) {
            // --- 表面 (Front): クロップ画像 ---
            Box(modifier = Modifier.fillMaxSize()) {
                ZoomableImage(
                    imagePath = cropImage.keyFramePath,
                    isPixelCoordinates = true,
                    initialTopLeft = Offset(cropImage.bboxLeft.toFloat(), cropImage.bboxTop.toFloat()),
                    initialBottomRight = Offset(cropImage.bboxRight.toFloat(), cropImage.bboxBottom.toFloat()),
                    modifier = Modifier.fillMaxSize()
                )

                // 左上: クラス名・スコアのバッジ
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cropImage.japaneseClassName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Score: %.2f".format(cropImage.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // 右下: フリップヒントガイド
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = "Flip Card",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "タップで裏面データ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        } else {
            // --- 裏面 (Back): 詳細データ表示 (鏡像反転補正) ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = 180f }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📊 検出オブジェクト詳細",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val timeStr = if (cropImage.realTime > 0L) {
                        SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault()).format(Date(cropImage.realTime))
                    } else if (realTimeMs > 0L) {
                        SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault()).format(Date(realTimeMs))
                    } else {
                        "%02d:%02d".format((currentTime / 3600).toInt(), ((currentTime % 3600) / 60).toInt())
                    }

                    val tableData = listOf(
                        "Timestamp" to timeStr,
                        "Class Name" to cropImage.japaneseClassName,
                        "Confidence Score" to "%.2f".format(cropImage.score),
                        "Color Tag" to cropImage.color.displayName,
                        "BBox Coordinates" to "[${cropImage.bboxLeft}, ${cropImage.bboxTop}, ${cropImage.bboxRight}, ${cropImage.bboxBottom}]",
                        "Frame Status" to "Active / Tracked"
                    )

                    VerticalTable(data = tableData)

                    // 右下: フリップヒントガイド
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        shape = CircleShape,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flip,
                                contentDescription = "Flip Back",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "タップで表面画像",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 画像がなくなった場合の空状態表示カード & リセットボタン
 */
@Composable
fun EmptyTinderCard(
    onReset: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🔍 対象の画像はありません",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "現在のフィルター条件で表示できるすべての画像がスワイプ・確認されました。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Re-check Cards",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("忘れ物を見落としていないか再確認")
            }
        }
    }
}

/**
 * Tinder 風アクションボタン (❌ DISLIKE / 🔄 FLIP または RESET / ❤️ LIKE)
 */
@Composable
fun TinderActionButtons(
    hasCards: Boolean,
    onDislike: () -> Unit,
    onFlip: () -> Unit,
    onReset: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dislikeBg = if (hasCards) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val dislikeContent = if (hasCards) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    val flipBg = MaterialTheme.colorScheme.secondaryContainer
    val flipContent = MaterialTheme.colorScheme.onSecondaryContainer

    val likeBg = if (hasCards) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val likeContent = if (hasCards) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ❌ DISLIKE ボタン
        IconButton(
            onClick = onDislike,
            enabled = hasCards,
            modifier = Modifier
                .size(52.dp)
                .background(dislikeBg, CircleShape)
                .border(1.5.dp, dislikeContent, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dislike",
                tint = dislikeContent,
                modifier = Modifier.size(26.dp)
            )
        }

        // 🔄 RESET / FLIP ボタン
        IconButton(
            onClick = if (hasCards) onFlip else onReset,
            modifier = Modifier
                .size(46.dp)
                .background(flipBg, CircleShape)
                .border(1.5.dp, flipContent, CircleShape)
        ) {
            Icon(
                imageVector = if (hasCards) Icons.Default.Flip else Icons.Default.Refresh,
                contentDescription = if (hasCards) "Flip Card" else "Reset Cards",
                tint = flipContent,
                modifier = Modifier.size(22.dp)
            )
        }

        // ❤️ LIKE ボタン
        IconButton(
            onClick = onLike,
            enabled = hasCards,
            modifier = Modifier
                .size(52.dp)
                .background(likeBg, CircleShape)
                .border(1.5.dp, likeContent, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = likeContent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
