package com.example.keyframeplayer.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keyframeplayer.model.Topic
import com.example.keyframeplayer.ui.viewmodel.ImageRecognitionViewModel
import com.example.keyframeplayer.util.VideoInfo
import kotlinx.coroutines.launch

enum class ManagementScreenState {
    SELECT_DIRECTORY,
    LOADING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryManagementScreen(
    currentUri: String,
    isAccessible: Boolean,
    videoInfos: List<VideoInfo>,
    isLoading: Boolean,
    onTopicClick: (Topic, List<VideoInfo>) -> Unit,
    onChooseClick: () -> Unit
) {
    val context = LocalContext.current
    val keyFrameViewModel: ImageRecognitionViewModel = viewModel()

    // rememberSaveable に変更して、詳細画面から戻っても状態を維持する
    var currentScreenState by rememberSaveable { mutableStateOf(ManagementScreenState.SELECT_DIRECTORY) }
    var confirmedVideoInfos by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedStartIndex by remember { mutableStateOf<Int?>(null) }
    var selectedEndIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(isAccessible, currentUri) {
        selectedStartIndex = null
        selectedEndIndex = null
    }

    val isFabEnabled = isAccessible && !isLoading && videoInfos.isNotEmpty()
    val scope = rememberCoroutineScope()

    when (currentScreenState) {
        ManagementScreenState.LOADING -> {
            VideoKeyFrameLoadingScreen(
                viewModel = keyFrameViewModel,
                currentUri = currentUri,
                isAccessible = isAccessible,
                videoInfos = confirmedVideoInfos,
                onTopicClick = onTopicClick,
                onChooseClick = onChooseClick
            )
        }
        ManagementScreenState.SELECT_DIRECTORY -> {
            Scaffold(
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { if (isFabEnabled) showBottomSheet = true },
                        expanded = isFabEnabled,
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                        text = { Text("解析範囲を選択") },
                        containerColor = if (isFabEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isFabEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            ) { innerPadding ->
                SelectDirectoryScreen(
                    currentUri = currentUri,
                    isAccessible = isAccessible,
                    isLoading = isLoading,
                    onChooseClick = onChooseClick,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            VideoRangeSelectorContent(
                videoInfos = videoInfos,
                startIndex = selectedStartIndex,
                endIndex = selectedEndIndex,
                onRangeSelected = { start, end ->
                    selectedStartIndex = start
                    selectedEndIndex = end
                },
                onConfirm = {
                    if (selectedStartIndex != null && selectedEndIndex != null) {
                        val start = minOf(selectedStartIndex!!, selectedEndIndex!!)
                        val end = maxOf(selectedStartIndex!!, selectedEndIndex!!)
                        confirmedVideoInfos = videoInfos.subList(start, end + 1)
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                            currentScreenState = ManagementScreenState.LOADING
                        }
                    } else {
                        Toast.makeText(context, "開始と終了を選択してください", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

@Composable
fun VideoRangeSelectorContent(
    videoInfos: List<VideoInfo>,
    startIndex: Int?,
    endIndex: Int?,
    onRangeSelected: (Int?, Int?) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "動画範囲の選択",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { onRangeSelected(0, videoInfos.size - 1) }) {
                Text("全選択")
            }
        }

        val rangeDesc = if (startIndex != null && endIndex != null) {
            val start = minOf(startIndex, endIndex)
            val end = maxOf(startIndex, endIndex)
            "${videoInfos[start].startTimeText} 〜 ${videoInfos[end].endTimeText} (${end - start + 1}本)"
        } else if (startIndex != null) {
            "終了動画をタップしてください..."
        } else {
            "開始動画をタップしてください"
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(rangeDesc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(videoInfos) { index, video ->
                val isStart = index == startIndex
                val isEnd = index == endIndex
                val inRange = if (startIndex != null && endIndex != null) {
                    index in minOf(startIndex, endIndex)..maxOf(startIndex, endIndex)
                } else false

                VideoItemCard(
                    video = video,
                    isSelected = isStart || isEnd,
                    isInRange = inRange,
                    onClick = {
                        if (startIndex == null || (startIndex != null && endIndex != null)) {
                            onRangeSelected(index, null)
                        } else {
                            onRangeSelected(startIndex, index)
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("キャンセル")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = startIndex != null && endIndex != null
            ) {
                Text("範囲を確定")
            }
        }
    }
}

@Composable
fun VideoItemCard(
    video: VideoInfo,
    isSelected: Boolean,
    isInRange: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isInRange -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = contentColor.copy(alpha = 0.7f))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${video.startTimeText} (${video.durationText})",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = contentColor)
            } else if (isInRange) {
                Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
