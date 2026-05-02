package com.example.keyframeplayer.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.keyframeplayer.KeyframeItem
import com.example.keyframeplayer.data.repository.KeyframeRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

class MainViewModel(
    private val repository: KeyframeRepository
) : ViewModel() {

    var keyframes by mutableStateOf<List<KeyframeItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun load(uri: Uri, context: Context) {
        viewModelScope.launch {
            isLoading = true
            keyframes = repository.loadAndSaveKeyframes(context, uri)
            isLoading = false
        }
    }
}