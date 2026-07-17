package com.example.keyframeplayer.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.ui.viewmodel.SaveKeyFrameViewModel
import com.example.keyframeplayer.util.VideoInfo

@Composable
fun VideoKeyFrameLoadingScreen(
    viewModel: SaveKeyFrameViewModel,
    currentUri: String,
    isAccessible: Boolean,
    videoInfos: List<VideoInfo>,
    onChooseClick: () -> Unit
) {
    // 1. ViewModel から「処理中か？」「今何％か？」の状態をリアルタイムに受け取る
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 通常の画面コンテンツ ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "動画のキーフレーム抽出画面")
            Spacer(modifier = Modifier.height(16.dp))

            // ボタンを押したら、ViewModel の関数を呼ぶだけ（Contextの受け渡しは不要！）
            Button(onClick = { viewModel.startProcessing(videoInfos) }) {
                Text("動画処理を開始")
            }
        }

        // --- 処理中のオーバーレイ表示 ---
        // isLoading が true のときだけ、画面全体に薄暗い膜と円状の進捗バーを重ねる
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    , //.background(Color.Black.copy(alpha = 0.4f)), // 背景を40%の黒透過に
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 円状の進捗バー（progressには 0.0〜1.0 の値が入る）
                    CircularProgressIndicator(progress = { progress })

                    Spacer(modifier = Modifier.height(12.dp))

                    // 「45% 処理中...」のようにテキスト表示
                    Text(
                        text = "${(progress * 100).toInt()}% 処理中...",
                        color = Color.White
                    )
                }
            }
        }
    }
}