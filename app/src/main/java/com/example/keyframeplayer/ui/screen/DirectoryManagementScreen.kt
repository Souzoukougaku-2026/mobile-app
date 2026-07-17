package com.example.keyframeplayer.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.keyframeplayer.ui.viewmodel.ImageRecognitionViewModel
import com.example.keyframeplayer.util.VideoInfo
import kotlinx.coroutines.launch

// --- 追加: 画面の状態を表すenum ---
enum class ManagementScreenState {
    SELECT_DIRECTORY, // 通常の画面（ボトムシート含む）
    LOADING           // 動画処理中のローディング画面
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryManagementScreen(
    currentUri: String,
    isAccessible: Boolean,
    videoInfos: List<VideoInfo>,
    isLoading: Boolean,
    onChooseClick: () -> Unit
) {
    val context = LocalContext.current
    val keyFrameViewModel: ImageRecognitionViewModel = viewModel()

    // --- 追加: 現在の画面状態を管理するState ---
    var currentScreenState by remember { mutableStateOf(ManagementScreenState.SELECT_DIRECTORY) }
    // --- 追加: 決定された動画リストを保持するState ---
    var confirmedVideoInfos by remember { mutableStateOf<List<VideoInfo>>(emptyList()) }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // 選択された動画を保持
    var selectedStartVideo by remember { mutableStateOf<VideoInfo?>(null) }
    var selectedEndVideo by remember { mutableStateOf<VideoInfo?>(null) }

    var startMenuExpanded by remember { mutableStateOf(false) }
    var endMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isAccessible, currentUri) {
        selectedStartVideo = null
        selectedEndVideo = null
    }

    val isFabEnabled = isAccessible && !isLoading && videoInfos.isNotEmpty()
    val scope = rememberCoroutineScope()

    when (currentScreenState) {
        ManagementScreenState.LOADING -> {
            // --- 追加: ローディング画面の呼び出し ---
            VideoKeyFrameLoadingScreen(
                viewModel = keyFrameViewModel,
                currentUri = currentUri,
                isAccessible = isAccessible,
                videoInfos = confirmedVideoInfos,
                onChooseClick = onChooseClick
            )
        }
        ManagementScreenState.SELECT_DIRECTORY -> {
            Scaffold(
                floatingActionButton = {
                    FloatingActionButton(
                        // 【修正2】 無効時は onClick を null にするか、処理をスキップする
                        onClick = { if (isFabEnabled) showBottomSheet = true },
                        // 【修正3】 有効・無効に合わせて色を切り替える
                        containerColor = if (isFabEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            ButtonDefaults.buttonColors().disabledContainerColor // 無効時の背景色（お好みで変更可）
                        },
                        contentColor = if (isFabEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            ButtonDefaults.buttonColors().disabledContentColor // 無効時のアイコン色
                        },
                        elevation = if (isFabEnabled) {
                            FloatingActionButtonDefaults.elevation()
                        } else {
                            FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open Range Form"
                        )
                    }
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
        CompositionLocalProvider(LocalRippleConfiguration provides null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "動画範囲選択",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // --- 1. 動画開始時間フォーム ---
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        OutlinedTextField(
                            value = selectedStartVideo?.startTimeText ?: "",
                            onValueChange = {},
                            label = { Text("開始時刻") },
                            trailingIcon = {
                                if (!selectedStartVideo?.startTimeText.isNullOrEmpty()) {
                                    IconButton(
                                        onClick = {
                                            selectedStartVideo = null
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "クリア"
                                        )
                                    }
                                }
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(
                                    onClick = { startMenuExpanded = !startMenuExpanded },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                )
                        )
                        if (!selectedStartVideo?.startTimeText.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 44.dp)
                                    .size(48.dp)
                                    .clickable(
                                        onClick = { selectedStartVideo = null },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    )
                            )
                        }

                        DropdownMenu(
                            expanded = startMenuExpanded,
                            onDismissRequest = { startMenuExpanded = false },
                            offset = DpOffset(0.dp, (-8).dp),
                            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 250.dp)
                        ) {
                            // 💡 インデックスによる制限: 終了動画が選択されている場合、その動画のインデックス以下の候補だけを残す
                            val endIdx = videoInfos.indexOf(selectedEndVideo)
                            val filteredStartList = if (endIdx != -1) {
                                videoInfos.take(endIdx + 1) // 終了動画と同じ、またはそれ以前
                            } else {
                                videoInfos
                            }

                            filteredStartList.forEach { video ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = video.startTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedStartVideo = video
                                        startMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "～",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp).graphicsLayer(rotationZ = 90f)
                    )

                    // --- 2. 動画終了時間フォーム ---
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        OutlinedTextField(
                            value = selectedEndVideo?.endTimeText ?: "",
                            onValueChange = {},
                            label = { Text("終了時刻") },
                            trailingIcon = {
                                if (!selectedEndVideo?.endTimeText.isNullOrEmpty()) {
                                    IconButton(
                                        onClick = {
                                            selectedEndVideo = null
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "クリア"
                                        )
                                    }
                                }
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(horizontal = 32.dp)
                                .clickable(
                                    onClick = { endMenuExpanded = !endMenuExpanded },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                )
                        )
                        if (!selectedEndVideo?.endTimeText.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 44.dp)
                                    .size(48.dp)
                                    .clickable(
                                        onClick = { selectedEndVideo = null },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    )
                            )
                        }

                        DropdownMenu(
                            expanded = endMenuExpanded,
                            onDismissRequest = { endMenuExpanded = false },
                            offset = DpOffset(0.dp, (-8).dp),
                            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 250.dp)
                        ) {
                            // 💡 インデックスによる制限: 開始動画が選択されている場合、その動画のインデックス以上の候補だけを残す
                            val startIdx = videoInfos.indexOf(selectedStartVideo)
                            val filteredEndList = if (startIdx != -1) {
                                videoInfos.drop(startIdx) // 開始動画と同じ、またはそれ以降
                            } else {
                                videoInfos
                            }

                            filteredEndList.forEach { video ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = video.endTimeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedEndVideo = video
                                        endMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp)) // 必要に応じて隙間を調整

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)

                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "キャンセル",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // アイコンとテキストの隙間
                            Text(
                                text = "キャンセル",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            onClick = {
                                if (selectedStartVideo != null && selectedEndVideo != null) {
                                    val startIdx = videoInfos.indexOf(selectedStartVideo)
                                    val endIdx = videoInfos.indexOf(selectedEndVideo)
                                    val selectedVideoInfos = videoInfos.subList(startIdx, endIdx + 1)

                                    // 【修正ポイント】
                                    // 1. 選択されたデータをStateに保存
                                    confirmedVideoInfos = selectedVideoInfos

                                    scope.launch {
                                        sheetState.hide()
                                        showBottomSheet = false
                                        // 2. ボトムシートが閉じた後に画面状態をLOADINGに切り替える
                                        currentScreenState = ManagementScreenState.LOADING
                                    }
                                } else {
                                    Toast.makeText(context, "動画範囲を選択してください", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "決定",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // アイコンとテキストの隙間
                            Text(
                                text = "決定",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}