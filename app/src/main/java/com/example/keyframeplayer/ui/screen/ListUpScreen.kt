package com.example.keyframeplayer.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.R
import com.example.keyframeplayer.data.DataSource
import com.example.keyframeplayer.model.Topic

/**
 * 独立したスクリーンコンポーザブル（他の画面やナビゲーションから呼び出し可能）
 */
@Composable
fun ListUpScreen(
    modifier: Modifier = Modifier
) {
    // -------------------------------------------------------------
    // 【入力および画面の状態管理（State）】
    // -------------------------------------------------------------
    var topics by remember { mutableStateOf(DataSource.topics) }
    var searchText by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // 検索入力時の絞り込み・優先度ソート処理
    fun onSearchTextChange(newText: String) {
        searchText = newText
        topics = DataSource.topics
            .filter {
                it.class_name.contains(searchText, ignoreCase = true)
            }
            .sortedByDescending {
                when {
                    it.class_name.equals(searchText, true) -> 100
                    it.class_name.startsWith(searchText, true) -> 80
                    it.class_name.contains(searchText, true) -> 60
                    else -> 0
                }
            }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                MyTopBar(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = it },
                    onSearchClick = { searchVisible = !searchVisible },
                    onSortByname = { topics = topics.sortedBy { it.class_name } },
                    onSortByDate = { topics = topics.sortedBy { it.fileTime } },
                    onFilterBlack = { topics = DataSource.topics.filter { it.imageColor == R.color.black } },
                    onFilterWhite = { topics = DataSource.topics.filter { it.imageColor == R.color.white } },
                    onFilterTime100 = { topics = DataSource.topics.filter { it.fileTime <= 100 } },
                    onFilterTime1000 = { topics = DataSource.topics.filter { it.fileTime >= 100 } },
                    onFiltername = { topics = DataSource.topics.filter { it.class_name == "photography" } },
                    onShowAll = { topics = DataSource.topics }
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
            TopicGrid(
                topics = topics,
                modifier = Modifier.padding(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                )
            )
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
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("ファイル名検索") },
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
        title = { Text("画像一覧") },
        actions = {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
            }
            IconButton(onClick = { onSearchClick() }) {
                Icon(Icons.Default.Search, contentDescription = "検索")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                Text("ソート", modifier = Modifier.padding(12.dp))
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
                Text("フィルタ", modifier = Modifier.padding(12.dp))
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
fun TopicGrid(topics: List<Topic>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(topics) { topic ->
            TopicCard(topic)
        }
    }
}

/**
 * 各カード要素コンポーネント
 */
@Composable
fun TopicCard(topic: Topic, modifier: Modifier = Modifier) {
    Card {
        Box {
            Image(
                painter = painterResource(id = topic.imageRes),
                contentDescription = null,
                modifier = modifier
                    .size(width = 200.dp, height = 100.dp)
                    .aspectRatio(2f),
                contentScale = ContentScale.Crop
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topic.class_name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        start = 16.dp,  // 16.dp に変更
                        top = 16.dp,    // 16.dp に変更
                        end = 16.dp,    // 16.dp に変更
                        bottom = 8.dp   // 8.dp に変更
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 【変更箇所】painterResource(R.drawable.ic_grain) の代わりに
                // Material Design 標準の Icon(imageVector = ...) を使用する
                Icon(
                    imageVector = Icons.Default.MoreVert, // または適当な標準アイコン
                    contentDescription = null,
                    modifier = Modifier.padding(start = 16.dp) // 16.dp に変更
                )
                Text(
                    text = topic.fileTime.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 8.dp) // 8.dp に変更
                )
            }
        }
    }
}