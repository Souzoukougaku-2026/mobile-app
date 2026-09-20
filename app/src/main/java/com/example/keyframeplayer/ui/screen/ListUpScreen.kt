package com.example.keyframeplayer.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.keyframeplayer.R
import com.example.keyframeplayer.model.Topic
import com.example.keyframeplayer.ui.viewmodel.ListUpViewModel
import java.io.File

/**
 * 独立したスクリーンコンポーザブル（他の画面やナビゲーションから呼び出し可能）
 */
@Composable
fun ListUpScreen(
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListUpViewModel = viewModel()
) {
    // -------------------------------------------------------------
    // 【入力および画面の状態管理（State）】
    // -------------------------------------------------------------
    val dbTopics by viewModel.topics.collectAsState()
    var displayTopics by remember(dbTopics) { mutableStateOf(dbTopics) }
    var searchText by remember { mutableStateOf("") }
    var searchVisible by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // 検索入力時の絞り込み・優先度ソート処理
    fun onSearchTextChange(newText: String) {
        searchText = newText
        displayTopics = dbTopics
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
                    onSortByname = { displayTopics = displayTopics.sortedBy { it.class_name } },
                    onSortByDate = { displayTopics = displayTopics.sortedBy { it.fileTime } },
                    onFilterBlack = { displayTopics = dbTopics.filter { it.imageColor == 3 } }, // 3 is BLACK in BaseColor enum
                    onFilterWhite = { displayTopics = dbTopics.filter { it.imageColor == 0 } }, // 0 is WHITE
                    onFilterTime100 = { displayTopics = dbTopics.filter { it.fileTime <= 100000 } },
                    onFilterTime1000 = { displayTopics = dbTopics.filter { it.fileTime >= 100000 } },
                    onFiltername = { displayTopics = dbTopics.filter { it.class_name == "photography" } },
                    onShowAll = { displayTopics = dbTopics }
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
                topics = displayTopics,
                onTopicClick = onTopicClick,
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
fun TopicGrid(
    topics: List<Topic>,
    onTopicClick: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(topics) { topic ->
            TopicCard(
                topic = topic,
                onClick = { onTopicClick(topic) }
            )
        }
    }
}

/**
 * 各カード要素コンポーネント
 */
@Composable
fun TopicCard(
    topic: Topic,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.clickable { onClick() }
    ) {
        Box {
            if (topic.imagePath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(topic.imagePath))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .aspectRatio(2f),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
                    error = painterResource(id = R.drawable.ic_launcher_foreground)
                )
            } else {
                Image(
                    painter = painterResource(id = topic.imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 200.dp, height = 100.dp)
                        .aspectRatio(2f),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // クラス名の表示
                Text(
                    text = topic.class_name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                // 認識された色を小さな円で表示
                val colorHex = when(topic.imageColor) {
                    0 -> Color.White
                    3 -> Color.Black
                    4 -> Color(0xFFFF0000) // RED
                    else -> Color.Gray
                }
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = colorHex,
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {}
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.width(4.dp))
                // フォーマットされた時間の表示
                Text(
                    text = topic.formattedTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
