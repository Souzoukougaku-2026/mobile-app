package com.example.a2026souzou.feature.list.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.a2026souzou.core.domain.model.CropImage
import com.example.a2026souzou.core.domain.model.ImageColor
import com.example.a2026souzou.core.util.DetectionSimulator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListRoute(
    viewModel: ListViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ListScreen(
        uiState = uiState,
        onAddResult = viewModel::addDetectionResult,
        onToggleClass = viewModel::toggleClass,
        onToggleColor = viewModel::toggleColor,
        onSetSortOrder = viewModel::setSortOrder
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    uiState: ListUiState,
    onAddResult: (com.example.a2026souzou.core.domain.model.DetectionResult) -> Unit,
    onToggleClass: (String) -> Unit,
    onToggleColor: (Int) -> Unit,
    onSetSortOrder: (Boolean) -> Unit
) {
    var showDebugMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("検出リスト") },
                actions = {
                    Box {
                        IconButton(onClick = { showDebugMenu = true }) {
                            Icon(Icons.Default.Add, contentDescription = "取得シミュレーション")
                        }
                        DropdownMenu(
                            expanded = showDebugMenu,
                            onDismissRequest = { showDebugMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("財布の検出を取得") },
                                onClick = {
                                    onAddResult(DetectionSimulator.acquireWalletDetection())
                                    showDebugMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("スマホの検出を取得") },
                                onClick = {
                                    onAddResult(DetectionSimulator.acquireSmartphoneDetection())
                                    showDebugMenu = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = { onSetSortOrder(!uiState.isAscending) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "ソート")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            FilterSection(uiState, onToggleClass, onToggleColor)
            
            HorizontalDivider()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("データがありません。+ボタンで取得をシミュレートしてください。")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.items) { item ->
                        CropImageItem(item)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    uiState: ListUiState,
    onToggleClass: (String) -> Unit,
    onToggleColor: (Int) -> Unit
) {
    val classes = CropImage.ALL_CLASSES
    val colors = ImageColor.entries

    Column(modifier = Modifier.padding(16.dp)) {
        Text("物体名:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            classes.forEach { name ->
                FilterChip(
                    selected = uiState.selectedClasses.contains(name),
                    onClick = { onToggleClass(name) },
                    label = { Text(name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("色:", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { color ->
                FilterChip(
                    selected = uiState.selectedColors.contains(color.id),
                    onClick = { onToggleColor(color.id) },
                    label = { Text(color.name) }
                )
            }
        }
    }
}

@Composable
fun CropImageItem(item: CropImage) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()) }
    val dateString = sdf.format(Date(item.realTime))

    ListItem(
        headlineContent = { Text("${item.className} (Score: ${"%.2f".format(item.score)})") },
        supportingContent = { 
            Text("Color: ${item.color.name} | Time: $dateString")
        },
        overlineContent = { Text("ID: ${item.id.toString().take(8)}...") }
    )
}
