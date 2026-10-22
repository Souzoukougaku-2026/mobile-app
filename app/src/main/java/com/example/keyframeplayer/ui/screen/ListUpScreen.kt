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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor
import com.example.keyframeplayer.core.domain.model.japaneseClassName
import com.example.keyframeplayer.core.domain.model.toJapaneseClassName
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
    onSortByDate: (Boolean) -> Unit = {},
    onToggleLiked: (UUID, Boolean) -> Unit = { _, _ -> },
    onNavigateToDetail: (CropImage) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsPerPage = 10

    // -------------------------------------------------------------
    // 【入力および画面の状態管理（State）】
    // -------------------------------------------------------------
    var searchText by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
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

    fun applySearch(text: String, currentItems: List<CropImage>): List<CropImage> {
        if (text.isBlank()) return currentItems
        return currentItems
            .filter {
                it.className.contains(text, ignoreCase = true) ||
                it.japaneseClassName.contains(text, ignoreCase = true) ||
                it.color.displayName.contains(text, ignoreCase = true) ||
                it.color.name.contains(text, ignoreCase = true)
            }
            .sortedByDescending {
                when {
                    it.japaneseClassName.equals(text, true) || it.className.equals(text, true) -> 100
                    it.japaneseClassName.startsWith(text, true) || it.className.startsWith(text, true) -> 80
                    it.japaneseClassName.contains(text, true) || it.className.contains(text, true) -> 60
                    else -> 0
                }
            }
    }

    // フィルター & ソートが適用された結果のリスト
    val filteredAndSortedItems = remember(
        items,
        searchText,
        selectedSortOption,
        selectedClassFilter,
        selectedColorFilter,
        isLikedOnlyFilter
    ) {
        var list = items

        // 1. 検索テキストフィルタ
        if (searchText.isNotBlank()) {
            list = applySearch(searchText, list)
        }

        // 2. いいね絞り込み
        if (isLikedOnlyFilter) {
            list = list.filter { it.isLiked }
        }

        // 3. クラス絞り込み
        if (selectedClassFilter != null) {
            list = list.filter { it.className.equals(selectedClassFilter, ignoreCase = true) }
        }

        // 4. 色絞り込み
        if (selectedColorFilter != null) {
            list = list.filter { it.color == selectedColorFilter }
        }

        // 5. ソート処理
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
                    onSearchClick = { searchVisible = !searchVisible },
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
                        searchText = ""
                        currentPage = 0
                    }
                )

                // 検索入力フィールド
                if (searchVisible) {
                    SearchInputField(
                        value = searchText,
                        onValueChange = {
                            searchText = it
                            currentPage = 0
                        }
                    )
                }
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
                    Text(
                        text = if (searchText.isNotBlank()) "検索結果が見つかりません" else "該当する検出画像がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
 * 検索入力用のコンポーネント
 */
@Composable
fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        placeholder = { Text("クラス名検索") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "検索"
            )
        },
        singleLine = true
    )
}

/**
 * トップバー（ヘッダー・三点リーダーメニュー）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
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

    // メニューが閉じられたらメインページに戻す
    LaunchedEffect(expanded) {
        if (!expanded) {
            currentMenuPage = MenuPage.MAIN
        }
    }

    TopAppBar(
        title = { Text("検出画像一覧") },
        actions = {
            IconButton(onClick = { onSearchClick() }) {
                Icon(Icons.Default.Search, contentDescription = "検索")
            }
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
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
                                    Text("🔀 ソート")
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
                                    Text("🏷️ クラス絞り込み")
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
                                    Text("🎨 色絞り込み")
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

                        // 4. ❤️ いいねのみ
                        DropdownMenuItem(
                            text = { Text(if (isLikedOnlyFilter) "❤️ いいねのみ [ON]" else "🤍 いいねのみ [OFF]") },
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
                            text = { Text("🔄 条件をクリア (すべて表示)", color = MaterialTheme.colorScheme.error) },
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
                                text = { Text(color.displayName) },
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
 * 各カード要素コンポーネント
 */
@Composable
fun TopicCard(
    topic: CropImage,
    onClick: () -> Unit,
    onToggleLiked: (UUID, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(topic.realTime, topic.fileTime) {
        if (topic.realTime > 0L) {
            SimpleDateFormat("yyyy/MM/dd,HH:mm:ss", Locale.getDefault()).format(Date(topic.realTime))
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
            ) {
                AsyncImage(
                    model = topic.keyFramePath,
                    contentDescription = topic.japaneseClassName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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
                        imageVector = if (topic.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (topic.isLiked) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = topic.japaneseClassName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "色: ${topic.color.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
