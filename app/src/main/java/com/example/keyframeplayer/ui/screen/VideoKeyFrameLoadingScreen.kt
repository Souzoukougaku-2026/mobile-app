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

    // 2. ViewModel から「処理中か？」「今何％か？」「詳細ステータス」「キーフレーム抽出完了フラグ」の状態を受け取る
    val progressState by viewModel.progressState.collectAsState()

    // 3. 画面遷移判定: キーフレーム抽出が完了したか、または全処理が完了したらリスト画面へ遷移
    val shouldNavigateToList = progressState.isKeyFrameExtractionFinished || (!progressState.isProcessing && progressState.progress >= 1.0f)

    if (shouldNavigateToList) {
        LaunchedEffect(Unit) { onFinished() }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                        progress = { progressState.progress },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "${(progressState.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = progressState.statusText.ifEmpty { "動画を準備中..." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
