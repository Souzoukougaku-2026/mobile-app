package com.example.a2026souzou.feature.movie.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.a2026souzou.feature.movie.presentation.components.*

@Composable
fun MovieRoute(
    viewModel: MovieViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    MovieScreen(
        uiState = uiState,
        onTimeChanged = viewModel::onTimeChanged,
        onProgressTapped = viewModel::onProgressTapped,
        onVisibleRangeChanged = viewModel::onVisibleRangeChanged,
    )
}

@Composable
fun MovieScreen(
    uiState: MovieUiState,
    onTimeChanged: (Float) -> Unit,
    onProgressTapped: (Float) -> Unit,
    onVisibleRangeChanged: (Float) -> Unit,
) {
    val scrollState = rememberScrollState()
    
    // スクロール位置の監視を最適化 (ANR対策)
    LaunchedEffect(scrollState) {
        snapshotFlow { 
            val totalContentWidth = scrollState.maxValue + scrollState.viewportSize
            if (totalContentWidth > 0) scrollState.value.toFloat() / totalContentWidth else 0f
        }
        .distinctUntilChanged { old: Float, new: Float ->
            // 変化が非常に小さい場合は無視して再描画を抑える (0.1% 未満の変化は無視)
            kotlin.math.abs(old - new) < 0.001f 
        }
        .collect { progress ->
            onVisibleRangeChanged(progress)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VideoPlaceholder()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // 1. 全体グラフ (Overall Graph)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "全体グラフ (Overall Graph)", 
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OverallGraphArea(uiState = uiState, onProgressTapped = onProgressTapped)
                
                OverallTimeTicks(uiState.totalDurationSeconds)
                
                SlimSlider(
                    value = uiState.currentTime,
                    onValueChange = onTimeChanged,
                    valueRange = 0f..uiState.totalDurationSeconds,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))

            // 2. 詳細グラフ (Detailed Graph)
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "詳細グラフ (Detailed Graph)", 
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                DetailedScrollableArea(
                    uiState = uiState,
                    scrollState = scrollState,
                    onTimeChanged = onTimeChanged,
                    onProgressTapped = onProgressTapped
                )
            }
            
            TimeLabel(uiState.currentTime, uiState.totalDurationSeconds, modifier = Modifier.padding(horizontal = 16.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            BottomInfoArea(uiState.currentTime)
        }
    }
}
