package com.example.a2026souzou.feature.list.presentation

import androidx.compose.runtime.Immutable
import com.example.a2026souzou.core.domain.model.CropImage

@Immutable
data class ListUiState(
    val items: List<CropImage> = emptyList(),
    val selectedClasses: Set<String> = emptySet(),
    val selectedColors: Set<Int> = emptySet(),
    val startTime: Long = 0L,
    val endTime: Long = System.currentTimeMillis() + 3600000 * 24, // +1 day
    val isAscending: Boolean = false,
    val isLoading: Boolean = false
)
