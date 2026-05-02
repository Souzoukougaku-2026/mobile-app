package com.example.keyframeplayer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.data.local.CropImageDao
import com.example.keyframeplayer.data.local.CropImageEntity
import kotlinx.coroutines.launch

@Composable
fun DebugScreen(
    dao: CropImageDao
) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<CropImageEntity>>(emptyList()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // ダミー追加（完全独立）
        Button(onClick = {
            scope.launch {
                dao.insert(
                    CropImageEntity(
                        cropImagePath = "debug://dummy",
                        className = "dummy",
                        score = 1.0f,
                        color = "red",
                        timeStampReal = System.currentTimeMillis(),
                        timeStampFile = System.currentTimeMillis(),
                        keyFramePath = null,
                        moviePath = null
                    )
                )
                list = dao.getAll()
            }
        }) {
            Text("ダミー追加")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 読み込み
        Button(onClick = {
            scope.launch {
                list = dao.getAll()
            }
        }) {
            Text("DB読み込み")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 全削除
        Button(onClick = {
            scope.launch {
                dao.deleteAll()
                list = emptyList()
            }
        }) {
            Text("全削除")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 表示
        LazyColumn {
            items(list) { item ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("class: ${item.className}")
                    Text("score: ${item.score}")
                    Text("time: ${item.timeStampFile}")
                }
            }
        }
    }
}