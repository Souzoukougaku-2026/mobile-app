/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.keyframeplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.data.DataSource
import com.example.keyframeplayer.model.Topic
import com.example.keyframeplayer.ui.theme.KeyframePlayerTheme
import kotlin.collections.sortedBy


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var topics by remember {
                mutableStateOf(DataSource.topics)
            }
            var searchText by remember {
                mutableStateOf("")
            }
            var searchVisible by remember {
                mutableStateOf(false)
            }
            var expanded by remember {
                mutableStateOf(false)
            }
            KeyframePlayerTheme {
                Scaffold(
                    topBar = {
                        Column{
                            MyTopBar(
                                expanded = expanded,
                                onExpandedChange = {
                                    expanded = it
                                },
                                onSearchClick = {
                                    searchVisible = !searchVisible
                                },
                                onSortByname = {
                                    topics = topics.sortedBy { it.class_name }
                                },
                                onSortByDate = {
                                    topics = topics.sortedBy { it.fileTime }
                                },
                                onFilterBlack = {
                                    topics = DataSource.topics.filter {
                                        it.imageColor == R.color.black
                                    }
                                },
                                onFilterWhite = {
                                    topics = DataSource.topics.filter {
                                        it.imageColor == R.color.white
                                    }
                                },
                                onFilterTime100 = {
                                    topics = DataSource.topics.filter {
                                        it.fileTime <= 100
                                    }
                                },
                                onFilterTime1000 = {
                                    topics = DataSource.topics.filter {
                                        it.fileTime >= 100
                                    }
                                },
                                onFiltername = {
                                    topics = DataSource.topics.filter{it.class_name == "photography"}
                                },
                                onShowAll = {
                                    topics = DataSource.topics
                                }
                            )
                            if(searchVisible){
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = {
                                        searchText = it
                                        topics =
                                            DataSource.topics
                                                .filter{
                                                    it.class_name.contains(
                                                        searchText,
                                                        ignoreCase = true
                                                    )
                                                }
                                                .sortedByDescending {
                                                    when{
                                                        it.class_name.equals(
                                                            searchText,
                                                            true
                                                        ) -> 100
                                                        it.class_name.startsWith(
                                                            searchText,
                                                            true
                                                        ) -> 80
                                                        it.class_name.contains(
                                                            searchText,
                                                            true
                                                        ) -> 60
                                                        else -> 0
                                                    }
                                                }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text("ファイル名検索")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search,
                                            contentDescription = null)
                                    }
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
                                start = dimensionResource(R.dimen.padding_small),
                                top = dimensionResource(R.dimen.padding_small),
                                end = dimensionResource(R.dimen.padding_small),
                            )
                        )
                    }
                }
            }
        }
    }
}
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
            IconButton(
                onClick = {
                    onExpandedChange(true)
                }
            ){
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "メニュー"
                )
            }
            IconButton(
                onClick = {
                    onSearchClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "検索"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    onExpandedChange(false)
                }
            ) {
                Text(
                    "ソート",
                    modifier = Modifier.padding(12.dp)
                )
                DropdownMenuItem(
                    text = {
                        Text("名前順")
                    },
                    onClick = {
                        onSortByname()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("時間順")
                    },
                    onClick = {
                        onSortByDate()
                        onExpandedChange(false)
                    }
                )
                HorizontalDivider()
                Text(
                    "フィルタ",
                    modifier = Modifier.padding(12.dp)
                )
                DropdownMenuItem(
                    text = {
                        Text("黒色のみ")
                    },
                    onClick = {
                        onFilterBlack()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("白色のみ")
                    },
                    onClick = {
                        onFilterWhite()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("100未満")
                    },
                    onClick = {
                        onFilterTime100()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("100以上")
                    },
                    onClick = {
                        onFilterTime1000()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("photographyのみ")
                    },
                    onClick = {
                        onFiltername()
                        onExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text("すべて表示")
                    },
                    onClick = {
                        onShowAll()
                        onExpandedChange(false)
                    }
                )
            }
        }
    )
}
@Composable
fun TopicGrid(topics: List<Topic>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small)),
        modifier = modifier
    ) {
        items(topics) { topic ->
            TopicCard(topic)
        }
    }
}

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
                    text = topic.class_name.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        start = dimensionResource(R.dimen.padding_medium),
                        top = dimensionResource(R.dimen.padding_medium),
                        end = dimensionResource(R.dimen.padding_medium),
                        bottom = dimensionResource(R.dimen.padding_small)
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_grain),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = dimensionResource(R.dimen.padding_medium))
                )
                Text(
                    text = topic.fileTime.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_small))
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopicPreview() {
    KeyframePlayerTheme {
        val topic = Topic("photography", 321, R.drawable.photography, imageColor=R.color.black)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopicCard(topic = topic)
        }
    }
}
