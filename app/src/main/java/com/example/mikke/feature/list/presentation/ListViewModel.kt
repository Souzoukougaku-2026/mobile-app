package com.example.mikke.feature.list.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mikke.core.data.repository.ProcessingProgressRepository
import com.example.mikke.core.data.repository.ProcessingProgressState
import com.example.mikke.core.domain.model.DetectionResult
import com.example.mikke.feature.list.domain.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor(
    private val repository: ListRepository,
    private val progressRepository: ProcessingProgressRepository
) : ViewModel() {

    val processingProgressState: StateFlow<ProcessingProgressState> = progressRepository.progressState

    private val _selectedClass = MutableStateFlow<String?>(null)
    private val _selectedColor = MutableStateFlow<Int?>(null)
    private val _timeRange = MutableStateFlow(0L to Long.MAX_VALUE)
    private val _isAscending = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ListUiState> = combine(
        _selectedClass,
        _selectedColor,
        _timeRange,
        _isAscending
    ) { clazz, color, range, isAsc ->
        repository.searchCropImages(
            className = clazz,
            color = color,
            startTime = range.first,
            endTime = range.second,
            isAsc = isAsc
        ).map { items ->
            ListUiState(
                items = items,
                selectedClasses = clazz?.let { setOf(it) } ?: emptySet(),
                selectedColors = color?.let { setOf(it) } ?: emptySet(),
                startTime = range.first,
                endTime = range.second,
                isAscending = isAsc,
                isLoading = false
            )
        }
    }.flatMapLatest { it }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListUiState(isLoading = true)
    )

    fun toggleClass(className: String) {
        _selectedClass.value = if (_selectedClass.value == className) null else className
    }

    fun toggleColor(color: Int) {
        _selectedColor.value = if (_selectedColor.value == color) null else color
    }

    fun setSortOrder(isAsc: Boolean) {
        _isAscending.value = isAsc
    }

    fun toggleLiked(id: java.util.UUID, isLiked: Boolean) {
        viewModelScope.launch {
            repository.updateLikedState(id, isLiked)
        }
    }

    /**
     * 保存済みの検出データをすべて削除し、完了後に onCleared を呼ぶ (最初からやり直す用)。
     */
    fun clearAllData(onCleared: () -> Unit) {
        viewModelScope.launch {
            progressRepository.clearAllPersistedData()
            onCleared()
        }
    }

    /**
     * 取得された検出結果（外部から渡された引数）をデータベースへ登録します。
     * この関数はデータの出元を問わず、受け取ったデータを保存する責務のみを持ちます。
     */
    fun addDetectionResult(result: DetectionResult) {
        viewModelScope.launch {
            repository.insertCropData(result.keyFrame, result.cropImages)
        }
    }
}

data class ListUiState(
    val items: List<com.example.mikke.core.domain.model.CropImage> = emptyList(),
    val selectedClasses: Set<String> = emptySet(),
    val selectedColors: Set<Int> = emptySet(),
    val startTime: Long = 0,
    val endTime: Long = Long.MAX_VALUE,
    val isAscending: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
