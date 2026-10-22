package com.example.keyframeplayer.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.keyframeplayer.ui.viewmodel.ImageRecognitionViewModel
import com.example.keyframeplayer.util.VideoInfo

@Composable
fun VideoKeyFrameLoadingScreen(
    viewModel: ImageRecognitionViewModel,
    currentUri: String,
    isAccessible: Boolean,
    videoInfos: List<VideoInfo>,
    onChooseClick: () -> Unit,
    onFinished: () -> Unit
) {
    // 1. 画面が表示されたタイミング、または videoInfos が準備できたタイミングで自動実行
    LaunchedEffect(videoInfos) {
        viewModel.startProcessing(videoInfos)
    }

    // 2. ViewModel から「処理中か？」「今何％か？」「詳細ステータス」の状態をリアルタイムに受け取る
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    // 3. 処理完了の判定（ローディング中でなく、進捗が100%に達しているか）
    val isCompleted = !isLoading && progress >= 1.0f

    // 4. 状態に応じて画面を分岐描画する
    if (isCompleted) {
        LaunchedEffect(Unit) { onFinished() }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (statusText.isNotEmpty()) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "動画を処理中...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}