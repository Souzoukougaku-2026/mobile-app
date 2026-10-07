package com.example.keyframeplayer.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.keyframeplayer.core.domain.model.CropImage
import com.example.keyframeplayer.core.domain.model.ImageColor

/**
 * 独立したスクリーンコンポーザブル（CropImageDBから取得したデータの表示）
 */
@Composable
fun ListUpScreen(
    items: List<CropImage>,
    onSortByDate: (Boolean) -> Unit,
    onNavigateToDetail: (CropImage) -> Unit,
    modifier: Modifier = Modifier
) {
    // -------------------------------------------------------------
    // 【入力および画面の状態管理（State）】
    // -------------------------------------------------------------
    var topics by remember(items) { mutableStateOf(items) }
    var searchText by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    fun applySearch(text: String, currentItems: List<CropImage>): List<CropImage> {
        if (text.isBlank()) return currentItems
        return currentItems
            .filter { it.className.contains(text, ignoreCase = true) }
            .sortedByDescending {
                when {
                    it.className.equals(text, true) -> 100
                    it.className.startsWith(text, true) -> 80
                    it.className.contains(text, true) -> 60
                    else -> 0
                }
            }
    }

    // 検索入力時の絞り込み・優先度ソート処理
    fun onSearchTextChange(newText: String) {
        searchText = newText
        topics = applySearch(newText, items)
    }

    LaunchedEffect(items) {
        topics = applySearch(searchText, items)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                MyTopBar(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                    onSearchClick = { searchVisible = !searchVisible },
                    onSortByname = { topics = topics.sortedBy { it.className } },
                    onSortByDate = { topics = topics.sortedBy { it.fileTime } },
                    onFilterBlack = { topics = items.filter { it.color == ImageColor.Black } },
                    onFilterWhite = { topics = items.filter { it.color == ImageColor.White } },
                    onFilterTime100 = { topics = items.filter { it.fileTime <= 100 } },
                    onFilterTime1000 = { topics = items.filter { it.fileTime >= 100 } },
                    onFiltername = { topics = items.filter { it.className.lowercase() == "photography" } },
                    onShowAll = { topics = items }
                )

                // 検索入力フィールド
                if (searchVisible) {
                    SearchInputField(
                        value = searchText,
                        onValueChange = { onSearchTextChange(it) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (topics.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchText.isNotBlank()) "検索結果が見つかりません" else "検出された画像がありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                TopicGrid(
                    topics = topics,
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
 * トップバー（ヘッダー・メニュー）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    onSortByname: () -> Unit,
    onSortByDate: () -> Unit,
    onFilterBlack: () -> Unit,
    onFilterWhite: () -> Unit,
    onFilterTime100: () -> Unit,
    onFilterTime1000: () -> Unit,
    onShowAll: () -> Unit,
    onFiltername: () -> Unit
) {
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
                Text("ソート", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
                DropdownMenuItem(
                    text = { Text("名前順") },
                    onClick = {
                        onSortByname()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("時間順") },
                    onClick = {
                        onSortByDate()
                        onExpandedChange(false)
                    }
                )
                HorizontalDivider()
                Text("フィルタ", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall)
                DropdownMenuItem(
                    text = { Text("黒色のみ") },
                    onClick = {
                        onFilterBlack()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("白色のみ") },
                    onClick = {
                        onFilterWhite()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("100未満") },
                    onClick = {
                        onFilterTime100()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("100以上") },
                    onClick = {
                        onFilterTime1000()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("photographyのみ") },
                    onClick = {
                        onFiltername()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("すべて表示") },
                    onClick = {
                        onShowAll()
                        onExpandedChange(false)
                    }
                )
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
            TopicCard(topic = topic, onClick = { onNavigateToDetail(topic) })
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
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = topic.keyFramePath,
                    contentDescription = topic.className,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = topic.className,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        text = topic.color.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Time: ${topic.fileTime}s",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}