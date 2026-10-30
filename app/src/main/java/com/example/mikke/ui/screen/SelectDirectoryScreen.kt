package com.example.mikke.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SelectDirectoryScreen(
    currentUri: String,
    isLoading: Boolean,
    onChooseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 画面の一番上から少し下げるための余白
        Spacer(modifier = Modifier.height(64.dp))

        // 1. 上部のタイトル (固定テキスト)
        Text(
            text = "フォルダを選択",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. 選択されたフォルダのパス表示
        // どのフォルダを選べばいいかの案内を、パスの上に表示する
        Text(
            text = "映像ファイル(mp4)が入ったフォルダの中身を表示して、そのフォルダを選択してください",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "選択されたパス:",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentUri,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 3. 選択ボタン
        Button(
            onClick = onChooseClick,
            enabled = !isLoading // 解析中は連打できないようボタンを制限
        ) {
            Text("フォルダを選択")
        }

        // 💡 4. ボタンの下にプログレスバー専用の「固定スペース」を確保
        // isLoading の真偽に関わらず常に 80.dp の高さをキープするため、レイアウトが崩れません
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp), // 高さを固定
            contentAlignment = Alignment.Center // 中央寄せ
        ) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "動画の時間を解析中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}