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
import androidx.compose.material.icons.filled.Flip
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.keyframeplayer.R
import com.example.keyframeplayer.core.domain.model.CropImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun BottomInfoArea(
    currentTime: Float,
    activeKeyframe: CropImage? = null,
    keyframes: List<CropImage> = emptyList(),
    realTimeMs: Long = 0L,
    onSwipeLeft: (CropImage?) -> Unit = {},
    onSwipeRight: (CropImage?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cardList = remember(activeKeyframe, keyframes) {
        if (activeKeyframe != null) {
            val rest = keyframes.filter { it.id != activeKeyframe.id }
            listOf(activeKeyframe) + rest
        } else {
            keyframes
        }
    }

    var currentIndex by remember(cardList) { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val currentCard = cardList.getOrNull(currentIndex)
    val nextCard = cardList.getOrNull(currentIndex + 1)

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
                text = "🔥 表:画像 / 裏:データ Tinder カード",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isFlipped) "📄 裏面 (詳細データ)" else "🖼️ 表面 (画像)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // カードスタック領域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            if (cardList.isEmpty() && activeKeyframe == null) {
                // デフォルトカード
                DefaultFlippableTinderCard(
                    currentTime = currentTime,
                    realTimeMs = realTimeMs,
                    isFlipped = isFlipped,
                    onToggleFlip = { isFlipped = !isFlipped }
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

                // 最前面（1枚目）のカード (フリップ & スワイプ可能)
                if (currentCard != null) {
                    SwipeableFlippableTinderCard(
                        cropImage = currentCard,
                        currentTime = currentTime,
                        realTimeMs = realTimeMs,
                        isFlipped = isFlipped,
                        onToggleFlip = { isFlipped = !isFlipped },
                        onSwipedLeft = {
                            onSwipeLeft(currentCard)
                            isFlipped = false
                            if (currentIndex < cardList.lastIndex) currentIndex++ else currentIndex = 0
                        },
                        onSwipedRight = {
                            onSwipeRight(currentCard)
                            isFlipped = false
                            if (currentIndex < cardList.lastIndex) currentIndex++ else currentIndex = 0
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tinder アクションボタン群 (❌ DISLIKE / 🔄 FLIP / ❤️ LIKE)
        TinderActionButtons(
            onDislike = {
                if (currentCard != null) {
                    onSwipeLeft(currentCard)
                    isFlipped = false
                    if (currentIndex < cardList.lastIndex) currentIndex++ else currentIndex = 0
                }
            },
            onFlip = {
                isFlipped = !isFlipped
            },
            onLike = {
                if (currentCard != null) {
                    onSwipeRight(currentCard)
                    isFlipped = false
                    if (currentIndex < cardList.lastIndex) currentIndex++ else currentIndex = 0
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
            // --- 表面 (Front): 画面いっぱいのクロップ画像 ---
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
                    color = Color.Black.copy(alpha = 0.65f),
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
                            text = cropImage.className.ifEmpty { "Object" },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Score: %.2f".format(cropImage.score),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
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
                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(cropImage.realTime))
                    } else if (realTimeMs > 0L) {
                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(realTimeMs))
                    } else {
                        "%02d:%02d".format((currentTime / 3600).toInt(), ((currentTime % 3600) / 60).toInt())
                    }

                    val tableData = listOf(
                        "Timestamp" to timeStr,
                        "Class Name" to cropImage.className,
                        "Confidence Score" to "%.2f".format(cropImage.score),
                        "Color Tag" to cropImage.color.name,
                        "BBox Coordinates" to "[${cropImage.bboxLeft}, ${cropImage.bboxTop}, ${cropImage.bboxRight}, ${cropImage.bboxBottom}]",
                        "Frame Status" to "Active / Tracked"
                    )

                    VerticalTable(data = tableData)

                    // 右下: フリップヒントガイド (表面へ)
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
 * デフォルトカード (データ未選択時)
 */
@Composable
fun DefaultFlippableTinderCard(
    currentTime: Float,
    realTimeMs: Long,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "DefaultCardFlip"
    )

    val density = LocalDensity.current

    ElevatedCard(
        modifier = Modifier
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        if (rotationY <= 90f) {
            Box(modifier = Modifier.fillMaxSize()) {
                ZoomableImage(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    initialTopLeft = Offset(50f, 80f),
                    initialBottomRight = Offset(80f, 50f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = 180f }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "📊 検出オブジェクト詳細",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val timeStr = if (realTimeMs > 0L) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(realTimeMs))
                    } else {
                        "%02d:%02d".format((currentTime / 3600).toInt(), ((currentTime % 3600) / 60).toInt())
                    }

                    val tableData = listOf(
                        "Timestamp" to timeStr,
                        "Class" to "-",
                        "Score" to "-",
                        "Color" to "-",
                        "Status" to "Normal"
                    )

                    VerticalTable(data = tableData)
                }
            }
        }
    }
}

/**
 * Tinder 風アクションボタン (❌ DISLIKE / 🔄 FLIP / ❤️ LIKE)
 */
@Composable
fun TinderActionButtons(
    onDislike: () -> Unit,
    onFlip: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ❌ DISLIKE ボタン (左へスワイプ)
        IconButton(
            onClick = onDislike,
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xFFFFEBEE), CircleShape)
                .border(1.5.dp, Color(0xFFE53935), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dislike",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(26.dp)
            )
        }

        // 🔄 FLIP ボタン (表裏ひっくり返し)
        IconButton(
            onClick = onFlip,
            modifier = Modifier
                .size(46.dp)
                .background(Color(0xFFE3F2FD), CircleShape)
                .border(1.5.dp, Color(0xFF1976D2), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Flip,
                contentDescription = "Flip Card",
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(22.dp)
            )
        }

        // ❤️ LIKE ボタン (右へスワイプ)
        IconButton(
            onClick = onLike,
            modifier = Modifier
                .size(52.dp)
                .background(Color(0xE8E8F5E9), CircleShape)
                .border(1.5.dp, Color(0xFF4CAF50), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
