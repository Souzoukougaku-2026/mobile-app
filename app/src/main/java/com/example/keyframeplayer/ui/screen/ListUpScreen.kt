package com.example.keyframeplayer.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.keyframeplayer.R
import com.example.keyframeplayer.core.data.repository.ProcessingProgressState
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.core.domain.model.japaneseClassName
import com.example.keyframeplayer.core.domain.model.toJapaneseClassName
import com.example.keyframeplayer.ui.component.BboxCropTransformation
import com.example.keyframeplayer.ui.component.ProcessingProgressBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

/**
 * ソート項目の定義
 */
enum class ListSortOption(val label: String) {
    DATE_DESC("日時 (新しい順)"),
    DATE_ASC("日時 (古い順)"),
    NAME_ASC("クラス名 (昇順)"),
    NAME_DESC("クラス名 (降順)")
}

/**
 * ドロップダウンメニューの画面階層
 */
private enum class MenuPage {
    MAIN,
    SORT,
    CLASS,
    COLOR
}

/**
 * 独立したスクリーンコンポーザブル（CropImageDBから取得したデータの表示）
 * 最大10件ずつのNext/Prevページネーション表示対応
 */
@Composable
fun ListUpScreen(
    items: List<CropImage>,
    processingProgressState: ProcessingProgressState = ProcessingProgressState(),
    onSortByDate: (Boolean) -> Unit = {},
    onToggleLiked: (UUID, Boolean) -> Unit = { _, _ -> },
    onNavigateToDetail: (CropImage) -> Unit,
    onRedoDetection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val itemsPerPage = 10

    // -------------------------------------------------------------
    // 【入力および画面の状態管理（State）】
    // -------------------------------------------------------------
    var menuExpanded by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }

    // MovieDetail (MovieScreen) と同等の絞り込み & ソート状態
    var selectedSortOption by remember { mutableStateOf(ListSortOption.DATE_DESC) }
    var selectedClassFilter by remember { mutableStateOf<String?>(null) }
    var selectedColorFilter by remember { mutableStateOf<ImageColor?>(null) }
    var isLikedOnlyFilter by remember { mutableStateOf(false) }

    // 動的なクラス・色の選択肢リスト
    val availableClasses = remember(items) {
        items.map { it.className }.distinct().sorted()
    }

    val availableColors = remember(items) {
        items.map { it.color }.distinct().sortedBy { it.name }
    }

    // フィルター & ソートが適用された結果のリスト
    val filteredAndSortedItems = remember(
        items,
        selectedSortOption,
        selectedClassFilter,
        selectedColorFilter,
        isLikedOnlyFilter
    ) {
        var list = items

        // 1. いいね絞り込み
        if (isLikedOnlyFilter) {
            list = list.filter { it.isLiked }
        }

        // 2. クラス絞り込み
        if (selectedClassFilter != null) {
            list = list.filter { it.className.equals(selectedClassFilter, ignoreCase = true) }
        }

        // 3. 色絞り込み
        if (selectedColorFilter != null) {
            list = list.filter { it.color == selectedColorFilter }
        }

        // 4. ソート処理
        when (selectedSortOption) {
            ListSortOption.DATE_DESC -> list.sortedByDescending { if (it.realTime > 0L) it.realTime else it.fileTime.toLong() }
            ListSortOption.DATE_ASC -> list.sortedBy { if (it.realTime > 0L) it.realTime else it.fileTime.toLong() }
            ListSortOption.NAME_ASC -> list.sortedBy { it.japaneseClassName }
            ListSortOption.NAME_DESC -> list.sortedByDescending { it.japaneseClassName }
        }
    }

    // 検索・フィルター変更時にページを0にリセット
    LaunchedEffect(filteredAndSortedItems.size) {
        currentPage = 0
    }

    val totalPages = remember(filteredAndSortedItems) {
        maxOf(1, ceil(filteredAndSortedItems.size / itemsPerPage.toDouble()).toInt())
    }

    val pagedTopics = remember(filteredAndSortedItems, currentPage) {
        filteredAndSortedItems.drop(currentPage * itemsPerPage).take(itemsPerPage)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                MyTopBar(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                    onRedoDetection = onRedoDetection,
                    isProcessing = processingProgressState.isProcessing,
                    selectedSortOption = selectedSortOption,
                    onSortOptionSelected = {
                        selectedSortOption = it
                        currentPage = 0
                    },
                    selectedClassFilter = selectedClassFilter,
                    availableClasses = availableClasses,
                    onClassFilterSelected = {
                        selectedClassFilter = it
                        currentPage = 0
                    },
                    selectedColorFilter = selectedColorFilter,
                    availableColors = availableColors,
                    onColorFilterSelected = {
                        selectedColorFilter = it
                        currentPage = 0
                    },
                    isLikedOnlyFilter = isLikedOnlyFilter,
                    onLikedFilterToggled = {
                        isLikedOnlyFilter = !isLikedOnlyFilter
                        currentPage = 0
                    },
                    onResetAllFilters = {
                        selectedSortOption = ListSortOption.DATE_DESC
                        selectedClassFilter = null
                        selectedColorFilter = null
                        isLikedOnlyFilter = false
                        currentPage = 0
                    }
                )

                // 処理中のみ、トップバーの下に2段目のバーとして進捗を表示する
                ProcessingProgressBar(
                    isProcessing = processingProgressState.isProcessing,
                    progress = processingProgressState.progress,
                    statusText = processingProgressState.statusText
                )

            }
        },
        bottomBar = {
            if (filteredAndSortedItems.isNotEmpty()) {
                PaginationBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    totalItems = filteredAndSortedItems.size,
                    onPrevClick = { if (currentPage > 0) currentPage-- },
                    onNextClick = { if (currentPage < totalPages - 1) currentPage++ }
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (filteredAndSortedItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (processingProgressState.isProcessing) {
                        // 処理中 (特にキーフレーム抽出フェーズ) はまだ検出結果が無いだけなので、
                        // 「該当なし」ではなく処理中であることを表示する
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    } else {
                        Text(
                            text = "該当する検出画像がありません",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                TopicGrid(
                    topics = pagedTopics,
                    onToggleLiked = onToggleLiked,
                    onNavigateToDetail = onNavigateToDetail,
                    modifier = Modifier.padding(
                        start = 8.dp,
                        top = 8.dp,
                        end = 8.dp,
                    )
                )
            }
        }
    }
}

/**
 * ページネーション用コントロールバー（Prev / Next ボタン）
 */
@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onPrevClick,
                enabled = currentPage > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前へ")
                Spacer(modifier = Modifier.width(4.dp))
                Text("前へ")
            }

            Text(
                text = "${currentPage + 1} / $totalPages (全 ${totalItems}件)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onNextClick,
                enabled = currentPage < totalPages - 1
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "次へ")
            }
        }
    }
}

/**
 * トップバー（ヘッダー・三点リーダーメニュー）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRedoDetection: () -> Unit,
    isProcessing: Boolean = false,
    selectedSortOption: ListSortOption,
    onSortOptionSelected: (ListSortOption) -> Unit,
    selectedClassFilter: String?,
    availableClasses: List<String>,
    onClassFilterSelected: (String?) -> Unit,
    selectedColorFilter: ImageColor?,
    availableColors: List<ImageColor>,
    onColorFilterSelected: (ImageColor?) -> Unit,
    isLikedOnlyFilter: Boolean,
    onLikedFilterToggled: () -> Unit,
    onResetAllFilters: () -> Unit
) {
    var currentMenuPage by remember { mutableStateOf(MenuPage.MAIN) }
    var showRedoConfirmDialog by remember { mutableStateOf(false) }

    // メニューが閉じられたらメインページに戻す
    LaunchedEffect(expanded) {
        if (!expanded) {
            currentMenuPage = MenuPage.MAIN
        }
    }

    TopAppBar(
        title = { Text("検出画像一覧") },
        actions = {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(Icons.Default.FilterAlt, contentDescription = "フィルター")
            }
            // やり直しは専用ボタンとしてヘッダーの一番右に置く (確認ダイアログがあるため誤タップの心配はない)
            IconButton(
                onClick = { showRedoConfirmDialog = true },
                enabled = !isProcessing
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "検出データを削除してやり直す")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                when (currentMenuPage) {
                    MenuPage.MAIN -> {
                        Text(
                            text = "ソート・フィルター",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. ソート
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ソート")
                                    Text(
                                        text = selectedSortOption.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            onClick = { currentMenuPage = MenuPage.SORT }
                        )

                        // 2. クラス絞り込み (MovieDetail流用)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("クラス絞り込み")
                                    Text(
                                        text = selectedClassFilter?.toJapaneseClassName() ?: "すべて",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            onClick = { currentMenuPage = MenuPage.CLASS }
                        )

                        // 3. 色絞り込み (MovieDetail流用)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("色絞り込み")
                                    Text(
                                        text = selectedColorFilter?.displayName ?: "すべて",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                            onClick = { currentMenuPage = MenuPage.COLOR }
                        )

                        // 4. 🐾 足跡（お気に入り）のみ
                        DropdownMenuItem(
                            text = { Text(if (isLikedOnlyFilter) "お気に入り(足跡)のみ [ON]" else "お気に入り(足跡)のみ [OFF]") },
                            trailingIcon = {
                                if (isLikedOnlyFilter) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onLikedFilterToggled()
                                onExpandedChange(false)
                            }
                        )

                        HorizontalDivider()

                        // 5. すべてリセット
                        DropdownMenuItem(
                            text = { Text("条件をクリア (すべて表示)", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onResetAllFilters()
                                onExpandedChange(false)
                            }
                        )
                    }

                    MenuPage.SORT -> {
                        DropdownMenuItem(
                            text = { Text("⬅ 戻る", fontWeight = FontWeight.Bold) },
                            onClick = { currentMenuPage = MenuPage.MAIN }
                        )
                        HorizontalDivider()
                        ListSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                trailingIcon = {
                                    if (selectedSortOption == option) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    onSortOptionSelected(option)
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }

                    MenuPage.CLASS -> {
                        DropdownMenuItem(
                            text = { Text("⬅ 戻る", fontWeight = FontWeight.Bold) },
                            onClick = { currentMenuPage = MenuPage.MAIN }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("すべてのクラス") },
                            trailingIcon = {
                                if (selectedClassFilter == null) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onClassFilterSelected(null)
                                onExpandedChange(false)
                            }
                        )
                        availableClasses.forEach { className ->
                            DropdownMenuItem(
                                text = { Text(className.toJapaneseClassName()) },
                                trailingIcon = {
                                    if (selectedClassFilter == className) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    onClassFilterSelected(className)
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }

                    MenuPage.COLOR -> {
                        DropdownMenuItem(
                            text = { Text("⬅ 戻る", fontWeight = FontWeight.Bold) },
                            onClick = { currentMenuPage = MenuPage.MAIN }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("すべての色") },
                            trailingIcon = {
                                if (selectedColorFilter == null) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            onClick = {
                                onColorFilterSelected(null)
                                onExpandedChange(false)
                            }
                        )
                        availableColors.forEach { color ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_footprint),
                                            contentDescription = color.displayName,
                                            tint = color.composeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(color.displayName)
                                    }
                                },
                                trailingIcon = {
                                    if (selectedColorFilter == color) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                onClick = {
                                    onColorFilterSelected(color)
                                    onExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    )

    // 「やり直し」の確認ダイアログ (削除は元に戻せないため、危険性を明示する)
    if (showRedoConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRedoConfirmDialog = false },
            title = { Text("検出データをすべて削除しますか？") },
            text = {
                Text(
                    "保存されている検出結果とキーフレーム画像はすべて削除され、元に戻すことはできません。\n" +
                        "動画ファイル自体は削除されません。\n" +
                        "削除後は、動画選択の画面からやり直しになります。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRedoConfirmDialog = false
                        onRedoDetection()
                    }
                ) {
                    Text("削除してやり直す", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRedoConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

/**
 * グリッド表示コンポーネント
 */
@Composable
fun TopicGrid(
    topics: List<CropImage>,
    onToggleLiked: (UUID, Boolean) -> Unit,
    onNavigateToDetail: (CropImage) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(
            items = topics,
            key = { it.id }
        ) { topic ->
            TopicCard(
                topic = topic,
                onClick = { onNavigateToDetail(topic) },
                onToggleLiked = onToggleLiked
            )
        }
    }
}

/**
 * bbox で切り抜いたサムネイル画像。
 * キーフレーム全体ではなく検出範囲を表示し、余白は背面 (黒背景) に任せる。
 *
 * @param cropPaddingRatio 切り抜き範囲を各辺に広げる割合 (0.1f = 幅・高さの10%ずつ)
 */
@Composable
fun BboxThumbnail(
    topic: CropImage,
    cropPaddingRatio: Float = 0.2f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val request = remember(
        topic.id,
        topic.keyFramePath,
        topic.bboxLeft,
        topic.bboxTop,
        topic.bboxRight,
        topic.bboxBottom,
        cropPaddingRatio
    ) {
        ImageRequest.Builder(context)
            .data(topic.keyFramePath)
            // bbox は元画像の絶対座標なので、縮小デコードせず元サイズで切り抜く
            .size(Size.ORIGINAL)
            .transformations(
                BboxCropTransformation(
                    bboxLeft = topic.bboxLeft,
                    bboxTop = topic.bboxTop,
                    bboxRight = topic.bboxRight,
                    bboxBottom = topic.bboxBottom,
                    paddingRatio = cropPaddingRatio
                )
            )
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = topic.japaneseClassName,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

/**
 * 各カード要素コンポーネント
 */
@Composable
fun TopicCard(
    topic: CropImage,
    onClick: () -> Unit,
    onToggleLiked: (UUID, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(topic.realTime, topic.fileTime) {
        if (topic.realTime > 0L) {
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date(topic.realTime))
        } else {
            "${topic.fileTime}s"
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    // Fit で生じる余白は黒で埋める
                    .background(Color.Black)
            ) {
                BboxThumbnail(
                    topic = topic,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = { onToggleLiked(topic.id, !topic.isLiked) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(if (topic.isLiked) R.drawable.ic_footprint_filled else R.drawable.ic_footprint),
                        contentDescription = "Favorite",
                        tint = if (topic.isLiked) Color(0xFFFF9800) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // 1段目: クラス名 (幅いっぱいに表示する)
                Text(
                    text = topic.japaneseClassName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 2段目: 日時と色
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_footprint),
                        contentDescription = topic.color.displayName,
                        tint = topic.color.composeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = topic.color.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
