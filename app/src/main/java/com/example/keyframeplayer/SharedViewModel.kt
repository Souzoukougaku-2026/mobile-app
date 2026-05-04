package com.example.keyframeplayer

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SharedViewModel : ViewModel() {
    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri

    private val _keyframeItems = MutableStateFlow<List<KeyframeItem>>(emptyList())
    val keyframeItems: StateFlow<List<KeyframeItem>> = _keyframeItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun selectVideo(uri: Uri) {
        _selectedUri.value = uri
        _keyframeItems.value = emptyList()
    }

    fun setKeyframeItems(items: List<KeyframeItem>) {
        _keyframeItems.value = items
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }
}
